package com.github.fzakaria.waterflow.poller;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.model.CancelTimerDecisionAttributes;
import com.amazonaws.services.simpleworkflow.model.CancelWorkflowExecutionDecisionAttributes;
import com.amazonaws.services.simpleworkflow.model.CompleteWorkflowExecutionDecisionAttributes;
import com.amazonaws.services.simpleworkflow.model.ContinueAsNewWorkflowExecutionDecisionAttributes;
import com.amazonaws.services.simpleworkflow.model.Decision;
import com.amazonaws.services.simpleworkflow.model.DecisionTask;
import com.amazonaws.services.simpleworkflow.model.DecisionType;
import com.amazonaws.services.simpleworkflow.model.FailWorkflowExecutionDecisionAttributes;
import com.amazonaws.services.simpleworkflow.model.HistoryEvent;
import com.amazonaws.services.simpleworkflow.model.PollForDecisionTaskRequest;
import com.amazonaws.services.simpleworkflow.model.RecordMarkerDecisionAttributes;
import com.amazonaws.services.simpleworkflow.model.RequestCancelActivityTaskDecisionAttributes;
import com.amazonaws.services.simpleworkflow.model.RequestCancelExternalWorkflowExecutionDecisionAttributes;
import com.amazonaws.services.simpleworkflow.model.RespondDecisionTaskCompletedRequest;
import com.amazonaws.services.simpleworkflow.model.ScheduleActivityTaskDecisionAttributes;
import com.amazonaws.services.simpleworkflow.model.SignalExternalWorkflowExecutionDecisionAttributes;
import com.amazonaws.services.simpleworkflow.model.StartChildWorkflowExecutionDecisionAttributes;
import com.amazonaws.services.simpleworkflow.model.StartTimerDecisionAttributes;
import com.amazonaws.services.simpleworkflow.model.TaskList;
import com.amazonaws.services.simpleworkflow.model.TypeAlreadyExistsException;
import com.github.fzakaria.waterflow.Workflow;
import com.github.fzakaria.waterflow.activity.ActivityMethod;
import com.github.fzakaria.waterflow.converter.DataConverter;
import com.github.fzakaria.waterflow.event.Event;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import static com.amazonaws.services.simpleworkflow.model.EventType.WorkflowExecutionCancelRequested;
import static com.github.fzakaria.waterflow.SwfUtil.makeKey;
import static com.github.fzakaria.waterflow.TaskType.WORKFLOW_EXECUTION;
import static com.github.fzakaria.waterflow.Workflow.createCompleteWorkflowExecutionDecision;
import static com.github.fzakaria.waterflow.Workflow.createFailWorkflowExecutionDecision;
import static com.github.fzakaria.waterflow.event.EventState.ERROR;
import static java.lang.String.format;


/**
 * Poll for {@link DecisionTask} event on a single domain and task list and ask a registered {@link Workflow} for next decisions.
 * <p/>
 * Implements {@link Runnable} so that multiple instances of this class can be scheduled to handle higher levels of activity tasks.
 *
 * @see BasePoller
 */
@Slf4j
@Value
@EqualsAndHashCode(callSuper = true)
@Accessors(fluent = true)
public class DecisionPoller extends BasePoller {
    private final Collection<Workflow> workflows = Lists.newArrayList();

    private final DataConverter dataConverter;

    /**
     * Construct a decision poller.
     * @param id unique id for poller used for logging and recording in SWF
     * @param domain SWF domain to poll
     * @param taskList SWF taskList to filter on
     * @param dataConverter used to serialize/deserialize input/output for workflows
     */
    public DecisionPoller(String id, String domain, String taskList, AmazonSimpleWorkflow swf, DataConverter dataConverter) {
        super(id, taskList,domain, swf);
        this.dataConverter = dataConverter;
    }

    /**
     * Register workflows added to this poller on Amazon SWF with this instance's domain and task list.
     * {@link TypeAlreadyExistsException} are ignored making this method idempotent.
     *
     * @see ActivityMethod
     */
    public void registerSwfWorkflows() {
        for (Workflow workflow : workflows) {
            try {
                workflow.domain(domain).taskList(taskList);
                swf.registerWorkflowType(workflow.createRegisterWorkflowTypeRequest());
                log.info(format("Register workflow succeeded %s", workflow));
            } catch (TypeAlreadyExistsException e) {
                log.info(format("Register workflow already exists %s", workflow));
            } catch (Throwable t) {
                String format = format("Register workflow failed %s", workflow);
                log.error(format);
                throw new IllegalStateException(format, t);
            }
        }
    }

    /**
     * Add the {@link Workflow} that need to be registered.
     * @param workflows The workflows that will be registered
     */
    public void addWorkflows(Workflow... workflows) {
        this.workflows.addAll(Lists.newArrayList(workflows));
    }

    @Override
    protected void poll() {
        // Events are request in newest-first reverse order;
        PollForDecisionTaskRequest request = createPollForDecisionTaskRequest();
        DecisionTask decisionTask = null;

        ImmutableList.Builder<HistoryEvent> historyBuilder = ImmutableList.builder();

        while (decisionTask == null || decisionTask.getNextPageToken() != null) {
            //If no decision task is available in the specified task list before the timeout of 60 seconds expires,
            //an empty result is returned. An empty result, in this context, means that a DecisionTask is returned,
            //but that the value of taskToken is an empty string.
            //{@see http://docs.aws.amazon.com/amazonswf/latest/apireference/API_PollForDecisionTask.html}
            decisionTask = swf.pollForDecisionTask(request);
            if (decisionTask.getEvents() != null) {
                historyBuilder.addAll(decisionTask.getEvents());
            }
            request.setNextPageToken(decisionTask.getNextPageToken());
        }

        //We sort here even though we expect it be sorted from the API just to be certain.
        final List<HistoryEvent> historyEvents = historyBuilder.build();
        final List<Event> events = historyEvents.stream().map(h -> new Event(h, historyEvents)).sorted().collect(Collectors.toList());
        if (events.isEmpty()) {
            log.debug("No decisions found for a workflow");
            return;
        }

        final Workflow workflow = lookupWorkflow(decisionTask).domain(domain)
                .dataConverter(dataConverter)
                .taskList(taskList)
                .workflowId(decisionTask.getWorkflowExecution().getWorkflowId())
                .runId(decisionTask.getWorkflowExecution().getRunId());

        workflow.init();
        workflow.events(events);

        // Finished loading history for this workflow, now ask it to make the next set of decisions.
        final String workflowId = decisionTask.getWorkflowExecution().getWorkflowId();
        final String runId = decisionTask.getWorkflowExecution().getRunId();

        final List<Decision> decisions = Lists.newArrayList();

        List<Event> workflowErrors = events.stream().filter(e -> e.task() == WORKFLOW_EXECUTION)
                .filter(e -> e.state() == ERROR).collect(Collectors.toList());

        if (workflowErrors.isEmpty()) {
            try {
                //No need to replay if cancel event exists. Just cancel immediately
                Optional<Event> cancelEvent = events.stream().filter(e -> e.type() == WorkflowExecutionCancelRequested).findFirst();
                cancelEvent.ifPresent(event -> workflow.onCancelRequested(event, decisions));

                CompletionStage<?> future =  workflow.decide(decisions);
                future.thenApply( r -> {
                    log.debug("Workflow {} completed. Added final decision to complete workflow.", workflow.key());
                    return dataConverter.toData(r);
                }).exceptionally(t -> {
                    log.debug("Workflow {} failed. Added final decision to complete workflow.", workflow.key());
                    return dataConverter.toData(t);
                }).thenAccept(r -> decisions.add(createCompleteWorkflowExecutionDecision(r)));

                if (decisions.isEmpty()) {
                    log.debug("{} no decisions", workflowId, runId);
                }

                decisions.stream().forEach( d -> log.debug("{} -> {}", workflowId, logNiceDecision(d)));

            } catch (Throwable t) {
                String runInfo = format("%s %s", workflowId, runId);
                String details = dataConverter.toData(t);
                log.error(runInfo, t);
                decisions.add(createFailWorkflowExecutionDecision(runInfo, t.getMessage(), details));
            }
        } else {
            String joinedErrors = Joiner.on('\n').join(workflowErrors);
            Decision failWorkflowExecutionDecision = createFailWorkflowExecutionDecision(format("%s %s", workflowId, runId), "Errors reported", joinedErrors);
            FailWorkflowExecutionDecisionAttributes attributes = failWorkflowExecutionDecision.getFailWorkflowExecutionDecisionAttributes();
            log.error("{}:\n\n{}", attributes.getReason(), attributes.getDetails());
            decisions.add(failWorkflowExecutionDecision);
        }

        try {
            swf.respondDecisionTaskCompleted(createRespondDecisionTaskCompletedRequest(decisionTask.getTaskToken(), decisions));
        } catch (Exception e) {
            log.error(format("%s: %s", workflowId, workflow), e);

        }
    }

    /**
     * Create a nice log message based on the {@link DecisionType} for the given decision.
     */
    public static String logNiceDecision(Decision decision) {
        String decisionType = decision.getDecisionType();
        switch (DecisionType.valueOf(decision.getDecisionType())) {
            case ScheduleActivityTask:
                ScheduleActivityTaskDecisionAttributes a1 = decision.getScheduleActivityTaskDecisionAttributes();
                return format("%s['%s' '%s': %s %s]", decisionType, a1.getActivityId(), a1.getActivityType().getName(), a1.getInput(), a1.getControl());
            case CompleteWorkflowExecution:
                CompleteWorkflowExecutionDecisionAttributes a2 = decision.getCompleteWorkflowExecutionDecisionAttributes();
                return format("%s[%s]", decisionType, a2.getResult());
            case FailWorkflowExecution:
                FailWorkflowExecutionDecisionAttributes a3 = decision.getFailWorkflowExecutionDecisionAttributes();
                return format("%s[%s %s]", decisionType, a3.getReason(), a3.getDetails());
            case CancelWorkflowExecution:
                CancelWorkflowExecutionDecisionAttributes a4 = decision.getCancelWorkflowExecutionDecisionAttributes();
                return format("%s[%s]", decisionType, a4.getDetails());
            case ContinueAsNewWorkflowExecution:
                ContinueAsNewWorkflowExecutionDecisionAttributes a5 = decision.getContinueAsNewWorkflowExecutionDecisionAttributes();
                return format("%s[%s]", decisionType, a5.getInput());
            case RecordMarker:
                RecordMarkerDecisionAttributes a6 = decision.getRecordMarkerDecisionAttributes();
                return format("%s['%s': %s]", decisionType, a6.getMarkerName(), a6.getDetails());
            case StartTimer:
                StartTimerDecisionAttributes a7 = decision.getStartTimerDecisionAttributes();
                return format("%s['%s': %s]", decisionType, a7.getTimerId(), a7.getControl());
            case CancelTimer:
                CancelTimerDecisionAttributes a8 = decision.getCancelTimerDecisionAttributes();
                return format("%s['%s']", decisionType, a8.getTimerId());
            case SignalExternalWorkflowExecution:
                SignalExternalWorkflowExecutionDecisionAttributes a9 = decision.getSignalExternalWorkflowExecutionDecisionAttributes();
                return format("%s['%s' wf='%s' runId='%s': '%s' '%s']", decisionType, a9.getSignalName(), a9.getWorkflowId(), a9.getRunId(), a9.getInput(), a9.getControl());
            case RequestCancelExternalWorkflowExecution:
                RequestCancelExternalWorkflowExecutionDecisionAttributes a10 = decision.getRequestCancelExternalWorkflowExecutionDecisionAttributes();
                return format("%s[wf='%s' runId='%s': '%s']", decisionType, a10.getWorkflowId(), a10.getRunId(), a10.getControl());
            case StartChildWorkflowExecution:
                StartChildWorkflowExecutionDecisionAttributes a11 = decision.getStartChildWorkflowExecutionDecisionAttributes();
                return format("%s['%s' '%s': '%s' '%s']", decisionType, a11.getWorkflowId(), a11.getWorkflowType().getName(), a11.getInput(), a11.getControl());
            case RequestCancelActivityTask:
                RequestCancelActivityTaskDecisionAttributes a12 = decision.getRequestCancelActivityTaskDecisionAttributes();
                return format("%s[%s]", decisionType, a12.getActivityId());
        }
        return null;
    }

    /**
     * find the registered workflow related to the current decision task
     */
    private Workflow lookupWorkflow(DecisionTask decisionTask) {
        String name = decisionTask.getWorkflowType().getName();
        String version = decisionTask.getWorkflowType().getVersion();
        String key = makeKey(name, version);
        return workflows.stream().filter(w -> w.key().equals(key)).findFirst()
                .orElseThrow(() -> new IllegalStateException(format("Received decision task for unregistered workflow %s", key)));
    }

    public RespondDecisionTaskCompletedRequest createRespondDecisionTaskCompletedRequest(String taskToken, List<Decision> decisions) {
        return new RespondDecisionTaskCompletedRequest()
                .withDecisions(decisions)
                .withTaskToken(taskToken);
    }

    public PollForDecisionTaskRequest createPollForDecisionTaskRequest() {
        return new PollForDecisionTaskRequest()
                .withDomain(domain)
                .withTaskList(new TaskList().withName(taskList))
                .withIdentity(getId())
                .withReverseOrder(true);
    }

}
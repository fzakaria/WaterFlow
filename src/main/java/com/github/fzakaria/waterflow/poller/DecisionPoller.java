package com.github.fzakaria.waterflow.poller;

import com.amazonaws.services.simpleworkflow.flow.common.WorkflowExecutionUtils;
import com.amazonaws.services.simpleworkflow.model.Decision;
import com.amazonaws.services.simpleworkflow.model.DecisionTask;
import com.amazonaws.services.simpleworkflow.model.FailWorkflowExecutionDecisionAttributes;
import com.amazonaws.services.simpleworkflow.model.HistoryEvent;
import com.amazonaws.services.simpleworkflow.model.PollForDecisionTaskRequest;
import com.amazonaws.services.simpleworkflow.model.RegisterWorkflowTypeRequest;
import com.amazonaws.services.simpleworkflow.model.RespondDecisionTaskCompletedRequest;
import com.amazonaws.services.simpleworkflow.model.TaskList;
import com.amazonaws.services.simpleworkflow.model.TypeAlreadyExistsException;
import com.github.fzakaria.waterflow.CreateRegisterWorkflowTypeRequestBuilder;
import com.github.fzakaria.waterflow.Workflow;
import com.github.fzakaria.waterflow.converter.DataConverter;
import com.github.fzakaria.waterflow.event.Event;
import com.github.fzakaria.waterflow.event.ImmutableEvent;
import com.github.fzakaria.waterflow.immutable.DecisionContext;
import com.github.fzakaria.waterflow.immutable.Key;
import com.github.fzakaria.waterflow.immutable.Name;
import com.github.fzakaria.waterflow.immutable.Version;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import static com.amazonaws.services.simpleworkflow.model.EventType.WorkflowExecutionCancelRequested;
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
@Value.Immutable
public abstract class DecisionPoller extends BasePoller {

    public abstract List<Workflow<?,?>> workflows();

    public abstract DataConverter dataConverter();

    /**
     * Register workflows added to this poller on Amazon SWF with this instance's domain and task list.
     * {@link TypeAlreadyExistsException} are ignored making this method idempotent.
     *
     */
    @Override
    public void register() {
        for (Workflow workflow : workflows()) {
            try {
                RegisterWorkflowTypeRequest request =
                        CreateRegisterWorkflowTypeRequestBuilder.builder().domain(domain())
                        .workflow(workflow).build();
                swf().registerWorkflowType(request);
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
            decisionTask = swf().pollForDecisionTask(request);
            if (decisionTask.getEvents() != null) {
                historyBuilder.addAll(decisionTask.getEvents());
            }
            request.setNextPageToken(decisionTask.getNextPageToken());
        }

        //We sort here even though we expect it be sorted from the API just to be certain.
        final List<HistoryEvent> historyEvents = historyBuilder.build();
        final List<Event> events = historyEvents.stream().map(h ->
                ImmutableEvent.builder().historyEvent(h).historyEvents(historyEvents).build())
                .sorted().collect(Collectors.toList());

        if (events.isEmpty()) {
            log.debug("No decisions found for a workflow");
            return;
        }

        final Workflow<?,?> workflow = lookupWorkflow(decisionTask);

        // Finished loading history for this workflow, now ask it to make the next set of decisions.
        final String workflowId = decisionTask.getWorkflowExecution().getWorkflowId();
        final String runId = decisionTask.getWorkflowExecution().getRunId();

        //Order here is important since decisionContext creates a new array
        final DecisionContext decisionContext = DecisionContext.create().addAllEvents(events);
        final List<Decision> decisions = decisionContext.decisions();

        List<Event> workflowErrors = events.stream().filter(e -> e.task() == WORKFLOW_EXECUTION)
                .filter(e -> e.state() == ERROR).collect(Collectors.toList());

        if (workflowErrors.isEmpty()) {
            try {
                //No need to replay if cancel event exists. Just cancel immediately
                Optional<Event> cancelEvent = events.stream().filter(e -> e.type() == WorkflowExecutionCancelRequested).findFirst();
                cancelEvent.ifPresent(event -> workflow.onCancelRequested(event, decisions));

                CompletionStage<?> future =  workflow.decide(decisionContext);
                future.thenApply( r -> {
                    log.debug("Workflow {} completed. Added final decision to complete workflow.", workflow.key());
                    return dataConverter().toData(r);
                }).exceptionally(t -> {
                    log.debug("Workflow {} failed. Added final decision to complete workflow.", workflow.key());
                    return dataConverter().toData(t);
                }).thenAccept(r -> decisions.add(createCompleteWorkflowExecutionDecision(r)));

                if (log.isDebugEnabled()) {
                    log.debug(WorkflowExecutionUtils.prettyPrintDecisions(decisions));
                }

            } catch (Throwable t) {
                String runInfo = format("%s %s", workflowId, runId);
                String details = dataConverter().toData(t);
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
            swf().respondDecisionTaskCompleted(createRespondDecisionTaskCompletedRequest(decisionTask.getTaskToken(), decisions));
        } catch (Exception e) {
            log.error(format("%s: %s", workflowId, workflow), e);

        }
    }

    /**
     * find the registered workflow related to the current decision task
     */
    private Workflow<?,?> lookupWorkflow(DecisionTask decisionTask) {
        String name = decisionTask.getWorkflowType().getName();
        String version = decisionTask.getWorkflowType().getVersion();
        Key key = Key.of(Name.of(name), Version.of(version));
        return workflows().stream().filter(w -> w.key().equals(key)).findFirst()
                .orElseThrow(() -> new IllegalStateException(format("Received decision task for unregistered workflow %s", key)));
    }

    public RespondDecisionTaskCompletedRequest createRespondDecisionTaskCompletedRequest(String taskToken, List<Decision> decisions) {
        return new RespondDecisionTaskCompletedRequest()
                .withDecisions(decisions)
                .withTaskToken(taskToken);
    }

    public PollForDecisionTaskRequest createPollForDecisionTaskRequest() {
        return new PollForDecisionTaskRequest()
                .withDomain(domain().value())
                .withTaskList(new TaskList().withName(taskList().value()))
                .withIdentity(name().value())
                .withReverseOrder(true);
    }

}
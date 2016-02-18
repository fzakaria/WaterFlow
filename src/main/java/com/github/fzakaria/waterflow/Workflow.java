package com.github.fzakaria.waterflow;

import com.amazonaws.services.simpleworkflow.model.CancelWorkflowExecutionDecisionAttributes;
import com.amazonaws.services.simpleworkflow.model.ChildPolicy;
import com.amazonaws.services.simpleworkflow.model.CompleteWorkflowExecutionDecisionAttributes;
import com.amazonaws.services.simpleworkflow.model.Decision;
import com.amazonaws.services.simpleworkflow.model.DecisionType;
import com.amazonaws.services.simpleworkflow.model.EventType;
import com.amazonaws.services.simpleworkflow.model.FailWorkflowExecutionDecisionAttributes;
import com.amazonaws.services.simpleworkflow.model.RecordMarkerDecisionAttributes;
import com.amazonaws.services.simpleworkflow.model.RegisterWorkflowTypeRequest;
import com.amazonaws.services.simpleworkflow.model.StartWorkflowExecutionRequest;
import com.amazonaws.services.simpleworkflow.model.TaskList;
import com.amazonaws.services.simpleworkflow.model.WorkflowType;
import com.github.fzakaria.waterflow.action.Action;
import com.github.fzakaria.waterflow.converter.DataConverter;
import com.github.fzakaria.waterflow.event.Event;
import com.github.fzakaria.waterflow.poller.DecisionPoller;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import static com.amazonaws.services.simpleworkflow.model.EventType.WorkflowExecutionStarted;
import static com.github.fzakaria.waterflow.SwfConstants.*;
import static com.github.fzakaria.waterflow.SwfUtil.*;
import static java.lang.String.format;

/**
 * Contains the high level overview of a particular workflow.
 * It is essentially the decider for a particular workflow
 */
@Data
@Slf4j
@Accessors(fluent = true)
@EqualsAndHashCode(of="key")
public abstract class Workflow<InputType,OutputType> {

    protected final String name;
    protected final String version;
    /**
     * Workflow name and version glued together to make a key.
     *
     * @see SwfUtil#makeKey
     */
    protected final String key;
    protected final TypeToken<InputType> inputType;
    protected final TypeToken<OutputType> outputType;
    private final List<String> tags = Lists.newArrayList();
    /**
     * @return List containing all {@link Event} for the current workflow.
     */
    private List<Event> events = Lists.newLinkedList();

    // Optional fields used for submitting workflow.
    /** Optional description to register with workflow */
    private String description;
    /**
     * The total duration for this workflow execution.
     * Pass null unit or duration &lt;= 0 for default timeout period of 365 days.
     * Default is 365 days.
     *
     * @see StartWorkflowExecutionRequest#executionStartToCloseTimeout
     */
    private String executionStartToCloseTimeout = SWF_TIMEOUT_YEAR;

    /**
     * Specifies the maximum duration of <b>decision</b> tasks for this workflow execution.
     * Pass null unit or duration &lt;= 0 for a timeout of NONE.
     * <p/>
     * Defaults to one minute, which should be plenty of time deciders that don't need to
     * connect to external services to make the next decision.
     *
     * @see StartWorkflowExecutionRequest#taskStartToCloseTimeout
     */
    private String taskStartToCloseTimeout = SWF_TIMEOUT_DECISION_DEFAULT;

    /**
     * defaults to TERMINATE
     *
     * @see StartWorkflowExecutionRequest#childPolicy
     */
    private ChildPolicy childPolicy = ChildPolicy.TERMINATE; // sensible default

    // Set by poller
    /** SWF domain */
    private String domain;
    /** SWF task list this workflow is/will be executed under */
    private String taskList;
    /** Domain-unique workflow execution identifier * */
    private String workflowId;
    /** SWF generated unique run id for a specific workflow execution. */
    private String runId;
    private DataConverter dataConverter;

    public Workflow(String name, String version, Class<InputType> inputClazz, Class<OutputType> outputClazz) {
        this.name = assertValidSwfValue(assertMaxLength(name, MAX_NAME_LENGTH));
        this.version = assertValidSwfValue(assertMaxLength(version, MAX_VERSION_LENGTH));
        this.key = makeKey(name, version);
        this.inputType = TypeToken.of(inputClazz);
        this.outputType = TypeToken.of(outputClazz);
    }

    public Workflow(String name, String version, TypeToken<InputType> inputType, TypeToken<OutputType> outputType) {
        this.name = assertValidSwfValue(assertMaxLength(name, MAX_NAME_LENGTH));
        this.version = assertValidSwfValue(assertMaxLength(version, MAX_VERSION_LENGTH));
        this.key = makeKey(name, version);
        this.inputType = inputType;
        this.outputType = outputType;
    }

    /**
     * Add more events to this workflow.
     * Called by {@link DecisionPoller} as it polls for the history for the current workflow to be decided.
     * <p/>
     * NOTE: Assumes the events are in descending order by {@link Event#id()}.
     *
     * @param events events to add
     */
    public void events(List<Event> events) {
        this.events.addAll(events);
    }

    /**
     * Reset instance to prepare for new set of history events.
     */
    public void reset() {
        events = Lists.newLinkedList();
    }

    /**
     * Convenience method that calls {@link #reset} then {@link #events}.
     * <p/>
     * NOTE: Assumes the events are in descending order by {@link Event#id()}.
     */
    public void replaceEvents(List<Event> events) {
        reset();
        events(events);
    }

    /**
     * If available, return the input string given to this workflow when it was initiated on SWF.
     * @return the input or null if not available
     */
    public CompletionStage<InputType> workflowInput() {
        return workflowStartedEvent().thenApply(e -> dataConverter.fromData(e.input(), inputType.getType()));
    }

    /**
     * If available return the start date of the workflow when it was initiated on SWF.
     * <p/>
     * @return the workflow start date or null if not available
     */
    public CompletionStage<Date> workflowStartDate() {
        return workflowStartedEvent().thenApply(e -> e.eventTimestamp().toDate());
    }


    private CompletionStage<Event> workflowStartedEvent() {
        return events().stream().filter(e -> e.type() == WorkflowExecutionStarted)
                .findFirst().map(CompletableFuture::completedFuture).orElse( new CompletableFuture<>());
    }


    /**
     * Register {@link Action} instances with this workflow so that {@link Action#workflow}
     * will be automatically called with this instance before each {@link #decide}.
     * <p/>
     * Actions that are created dynamically within the {@link #decide} method will have to have
     * {@link Action#workflow()} called directly.
     *
     * @see Action#workflow
     */
    protected void actions(Action... actions) {
        for (Action action : actions) {
            action.workflow(this);
        }
    }

    /**
     * Subclasses add zero or more decisions to the parameter during a decision task.
     * A final {@link DecisionType#CompleteWorkflowExecution} or  {@link DecisionType#FailWorkflowExecution}
     * should be returned to indicate the workflow is complete. These decisions can be added by
     * {@link Action} instances automatically given their final state.
     * <p/>
     * An {@link DecisionType#FailWorkflowExecution} decision will be decided by the {@link DecisionPoller} if an unhandled exception is thrown
     * by this method.
     *
     * @see #createWorkflowExecutionRequest
     * @see #createFailWorkflowExecutionDecision
     */
    public abstract CompletionStage<OutputType> decide(List<Decision> decisions);

    /**
     * Called if an external process issued a {@link EventType#WorkflowExecutionCancelRequested} for this workflow.
     * By default will simply add a {@link DecisionType#CancelWorkflowExecution} decision.  Subclasses
     * can override this method to gracefully shut down a more complex workflow.
     */
    public void onCancelRequested(Event cancelEvent, List<Decision> decisions) {
        decisions.add(createCancelWorkflowExecutionDecision(cancelEvent.details()));
    }

    /**
     * Called by {@link DecisionPoller} to initialize workflow for a new decision task.
     */
    public void init() {
        events = Lists.newLinkedList();
    }


    /** SWF domain */
    public Workflow<InputType,OutputType> domain(String domain) {
        this.domain = assertValidSwfValue(assertMaxLength(domain, MAX_NAME_LENGTH));
        return this;
    }

    /** Domain-unique workflow execution identifier * */
    public Workflow<InputType,OutputType> workflowId(String workflowId) {
        this.workflowId = assertValidSwfValue(assertMaxLength(workflowId, MAX_ID_LENGTH));
        return this;
    }


    /** SWF generated unique run id for a specific workflow execution. */
    public Workflow<InputType,OutputType> runId(String runId) {
        this.runId = assertMaxLength(runId, MAX_RUN_ID_LENGTH);
        return this;
    }

    /** SWF task list this workflow is/will be executed under */
    public Workflow<InputType,OutputType> taskList(String taskList) {
        this.taskList = assertValidSwfValue(assertMaxLength(taskList, MAX_NAME_LENGTH));
        return this;
    }

    /** Optional tags submitted with workflow */
    public Workflow<InputType,OutputType> tags(Collection<String> tags) {
        for (String tag : tags) {
            this.tags.add(assertMaxLength(tag, MAX_NAME_LENGTH));
        }
        if (this.tags.size() > MAX_NUMBER_TAGS) {
            throw new AssertionError(format("More than %d tags not allowed, received: %s", MAX_NUMBER_TAGS, Joiner.on(",").join(tags)));
        }
        return this;
    }

    /** Optional tags submitted with workflow */
    public Workflow<InputType,OutputType> addTags(String... tags) {
        return tags(Arrays.asList(tags));
    }

    /** Optional description to register with workflow */
    public Workflow<InputType,OutputType> description(String description) {
        this.description = assertMaxLength(description, MAX_DESCRIPTION_LENGTH);
        return this;
    }

    /**
     * The total duration for this workflow execution.
     * Pass null unit or duration &lt;= 0 for default timeout period of 365 days.
     * Default is 365 days.
     *
     * @see StartWorkflowExecutionRequest#executionStartToCloseTimeout
     */
    public Workflow<InputType,OutputType> executionStartToCloseTimeout(TimeUnit unit, long duration) {
        executionStartToCloseTimeout = calcTimeoutOrYear(unit, duration);
        return this;
    }

    /**
     * Specifies the maximum duration of <b>decision</b> tasks for this workflow execution.
     * Pass null unit or duration &lt;= 0 for a timeout of NONE.
     * <p/>
     * Defaults to one minute, which should be plenty of time deciders that don't need to
     * connect to external services to make the next decision.
     *
     * @see StartWorkflowExecutionRequest#taskStartToCloseTimeout
     */
    public Workflow<InputType,OutputType> taskStartToCloseTimeout(TimeUnit unit, long duration) {
        this.taskStartToCloseTimeout = calcTimeoutOrNone(unit, duration);
        return this;
    }


    public StartWorkflowExecutionRequest createWorkflowExecutionRequest(String workflowId, String input) {
        return new StartWorkflowExecutionRequest()
                .withWorkflowId(workflowId)
                .withDomain(domain)
                .withTaskList(new TaskList()
                        .withName(taskList))
                .withWorkflowType(new WorkflowType()
                        .withName(name)
                        .withVersion(version))
                .withInput(input)
                .withTagList(tags)
                .withExecutionStartToCloseTimeout(executionStartToCloseTimeout)
                .withTaskStartToCloseTimeout(taskStartToCloseTimeout)
                .withChildPolicy(childPolicy == null ? null : childPolicy.name());
    }

    public RegisterWorkflowTypeRequest createRegisterWorkflowTypeRequest() {
        return new RegisterWorkflowTypeRequest()
                .withDomain(domain)
                .withDefaultTaskList(new TaskList().withName(taskList))
                .withName(name)
                .withVersion(version)
                .withDefaultExecutionStartToCloseTimeout(executionStartToCloseTimeout)
                .withDefaultTaskStartToCloseTimeout(taskStartToCloseTimeout)
                .withDefaultChildPolicy(childPolicy == null ? null : childPolicy.name())
                .withDescription(description)
                ;
    }

    public static Decision createCompleteWorkflowExecutionDecision(String result) {
        return new Decision()
                .withDecisionType(DecisionType.CompleteWorkflowExecution)
                .withCompleteWorkflowExecutionDecisionAttributes(
                        new CompleteWorkflowExecutionDecisionAttributes()
                                .withResult(trimToMaxLength(result, MAX_DETAILS_LENGTH))
                );
    }

    public static Decision createCancelWorkflowExecutionDecision(String result) {
        return new Decision()
                .withDecisionType(DecisionType.CancelWorkflowExecution)
                .withCancelWorkflowExecutionDecisionAttributes(
                        new CancelWorkflowExecutionDecisionAttributes()
                                .withDetails(trimToMaxLength(result, MAX_DETAILS_LENGTH)));
    }

    /**
     * Create a fail workflow reason by combining a target name and message.
     *
     * @param target target name, optional, usually the <code>toString()</code> of the object that caused an error.
     * @param message error message
     *
     * @return message if target is null, otherwise target and message combined into a single string
     */
    public static String createFailReasonString(String target, String message) {
        String fail = target == null ? message : format("%s:\n%s", target, message);
        return trimToMaxLength(fail, MAX_REASON_LENGTH);
    }

    public static Decision createFailWorkflowExecutionDecision(String target, String reason, String details) {
        return new Decision()
                .withDecisionType(DecisionType.FailWorkflowExecution)
                .withFailWorkflowExecutionDecisionAttributes(
                        new FailWorkflowExecutionDecisionAttributes()
                                .withReason(createFailReasonString(target, reason))
                                .withDetails(trimToMaxLength(details, MAX_DETAILS_LENGTH))
                );
    }

    public static Decision createRecordMarkerDecision(String name, Optional<String> details) {
        return new Decision()
                .withDecisionType(DecisionType.RecordMarker)
                .withRecordMarkerDecisionAttributes(new RecordMarkerDecisionAttributes()
                                .withMarkerName(trimToMaxLength(name, MARKER_NAME_MAX_LENGTH))
                                .withDetails(trimToMaxLength(details.orElse(""), MAX_DETAILS_LENGTH))
                );
    }

}
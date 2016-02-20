package com.github.fzakaria.waterflow;

import com.amazonaws.services.simpleworkflow.model.CancelWorkflowExecutionDecisionAttributes;
import com.amazonaws.services.simpleworkflow.model.ChildPolicy;
import com.amazonaws.services.simpleworkflow.model.CompleteWorkflowExecutionDecisionAttributes;
import com.amazonaws.services.simpleworkflow.model.Decision;
import com.amazonaws.services.simpleworkflow.model.DecisionType;
import com.amazonaws.services.simpleworkflow.model.EventType;
import com.amazonaws.services.simpleworkflow.model.FailWorkflowExecutionDecisionAttributes;
import com.amazonaws.services.simpleworkflow.model.StartWorkflowExecutionRequest;
import com.github.fzakaria.waterflow.action.Action;
import com.github.fzakaria.waterflow.converter.DataConverter;
import com.github.fzakaria.waterflow.event.Event;
import com.github.fzakaria.waterflow.immutable.DecisionContext;
import com.github.fzakaria.waterflow.immutable.Description;
import com.github.fzakaria.waterflow.immutable.Key;
import com.github.fzakaria.waterflow.immutable.Name;
import com.github.fzakaria.waterflow.immutable.TaskListName;
import com.github.fzakaria.waterflow.immutable.Version;
import com.github.fzakaria.waterflow.poller.DecisionPoller;
import com.google.common.base.Preconditions;
import com.google.common.reflect.TypeToken;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static com.amazonaws.services.simpleworkflow.model.EventType.WorkflowExecutionStarted;
import static com.github.fzakaria.waterflow.swf.SwfConstants.*;
import static com.github.fzakaria.waterflow.swf.SwfUtil.trimToMaxLength;
import static java.lang.String.format;

/**
 * Contains the high level overview of a particular workflow.
 * It is essentially the decider for a particular workflow
 */
public abstract class Workflow<InputType,OutputType> {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    public abstract Name name();

    public abstract Version version();
    /**
     * Workflow name and version glued together to make a key.
     */
    @Value.Derived
    public Key key() {
        return Key.of(name(), version());
    }

    public abstract TypeToken<InputType> inputType();

    public abstract TypeToken<OutputType> outputType();

    public abstract DataConverter dataConverter();

    /** Optional description to register with workflow */
    @Value.Default
    public Description description() {
        return DEFAULT_DESCRIPTION;
    }

    /**
     * The total duration for this workflow execution.
     * Pass null unit or duration &lt;= 0 for default timeout period of 365 days.
     * Default is 365 days.
     *
     * @see StartWorkflowExecutionRequest#executionStartToCloseTimeout
     */
    @Value.Default
    public Duration executionStartToCloseTimeout() {
        return SWF_TIMEOUT_YEAR;
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
    @Value.Default
    public Duration taskStartToCloseTimeout() {
        return SWF_TIMEOUT_DECISION_DEFAULT;
    }

    /**
     * defaults to TERMINATE
     *
     * @see StartWorkflowExecutionRequest#childPolicy
     */
    @Value.Default
    public ChildPolicy childPolicy() {
        return ChildPolicy.TERMINATE; // sensible default
    }

    /** SWF task list this workflow is/will be executed under if none given in submissoin*/
    @Value.Default
    public TaskListName taskList() {
        return DEFAULT_TASK_LIST;
    };

    @Value.Check
    protected void check() {
        Preconditions.checkState(executionStartToCloseTimeout().compareTo(SWF_TIMEOUT_YEAR) <= 0,
                "'executionStartToCloseTimeout' is longer than supported max timeout");
    }

    /**
     * If available, return the input string given to this workflow when it was initiated on SWF.
     * @return the input or null if not available
     */
    public CompletionStage<InputType> workflowInput(List<Event> events) {
        return workflowStartedEvent(events).thenApply(e -> dataConverter().fromData(e.input(), inputType().getType()));
    }

    /**
     * If available return the start date of the workflow when it was initiated on SWF.
     * <p/>
     * @return the workflow start date or null if not available
     */
    public CompletionStage<Instant> workflowStartDate(List<Event> events) {
        return workflowStartedEvent(events).thenApply(e -> e.eventTimestamp());
    }


    private CompletionStage<Event> workflowStartedEvent(List<Event> events) {
        return events.stream().filter(e -> e.type() == WorkflowExecutionStarted)
                .findFirst().map(CompletableFuture::completedFuture).orElse( new CompletableFuture<>());
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
     * @see #createFailWorkflowExecutionDecision
     */
    public abstract CompletionStage<OutputType> decide(DecisionContext decisionContext);

    /**
     * Called if an external process issued a {@link EventType#WorkflowExecutionCancelRequested} for this workflow.
     * By default will simply add a {@link DecisionType#CancelWorkflowExecution} decision.  Subclasses
     * can override this method to gracefully shut down a more complex workflow.
     */
    public void onCancelRequested(Event cancelEvent, List<Decision> decisions) {
        decisions.add(createCancelWorkflowExecutionDecision(cancelEvent.details()));
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
        String fail = target == null ? message : format("%s:%n%s", target, message);
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

}
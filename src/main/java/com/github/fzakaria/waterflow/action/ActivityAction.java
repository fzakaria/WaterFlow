package com.github.fzakaria.waterflow.action;

import com.amazonaws.services.simpleworkflow.model.ActivityType;
import com.amazonaws.services.simpleworkflow.model.Decision;
import com.amazonaws.services.simpleworkflow.model.DecisionType;
import com.amazonaws.services.simpleworkflow.model.ScheduleActivityTaskDecisionAttributes;
import com.amazonaws.services.simpleworkflow.model.TaskList;
import com.github.fzakaria.waterflow.TaskType;
import com.github.fzakaria.waterflow.converter.DataConverterException;
import com.github.fzakaria.waterflow.event.Event;
import com.github.fzakaria.waterflow.event.EventState;
import com.github.fzakaria.waterflow.immutable.DecisionContext;
import com.github.fzakaria.waterflow.immutable.Name;
import com.github.fzakaria.waterflow.immutable.TaskListName;
import com.github.fzakaria.waterflow.immutable.Version;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static com.github.fzakaria.waterflow.SwfConstants.DEFAULT_TASK_LIST;
import static com.github.fzakaria.waterflow.SwfConstants.SWF_TIMEOUT_NONE;
import static java.lang.String.format;

/**
 * The implementation of an SWF Activity.
 * An {@link OutputType} is needed to help the {@link com.github.fzakaria.waterflow.converter.DataConverter}
 * serialize/deserialize the output.
 */
public abstract class ActivityAction<OutputType> extends Action<OutputType> {

    /**
     * The name for this Activity
     * @see com.github.fzakaria.waterflow.activity.ActivityMethod
     */
    public abstract Name name();

    /**
     * The version for this Activity
     * @see com.github.fzakaria.waterflow.activity.ActivityMethod
     */
    public abstract Version version();

    /**
     * Override activity's default heartbeat timeout.
     * Pass null unit or duration &lt;= 0 for a timeout of NONE.
     *
     * @see ScheduleActivityTaskDecisionAttributes#heartbeatTimeout
     */
    @Value.Default
    public String heartBeatTimeoutTimeout() {
        return SWF_TIMEOUT_NONE;
    }

    /**
     * Override activity's default schedule to close timeout.
     * <pre>
     * schedule ---> start ---> close
     * |_____________________________|
     * </pre>
     * Pass null unit or duration &lt;= 0 for a timeout of NONE.
     *
     * @see ScheduleActivityTaskDecisionAttributes#scheduleToCloseTimeout
     */
    @Value.Default
    public String scheduleToCloseTimeout() {
        return SWF_TIMEOUT_NONE;
    }

    /**
     * Override activity's default schedule to close timeout.
     * <pre>
     * schedule ---> start ---> close
     * |_________________|
     * </pre>
     * Pass null unit or duration &lt;= 0 for a timeout of NONE.
     *
     * @see ScheduleActivityTaskDecisionAttributes#scheduleToStartTimeout
     */
    @Value.Default
    public String scheduleToStartTimeout() {
        return SWF_TIMEOUT_NONE;
    }


    /**
     * Override activity's default start to close timeout.
     * <pre>
     * schedule ---> start ---> close
     *              |_______________|
     * </pre>
     * Pass null unit or duration &lt;= 0 for a timeout of NONE.
     *
     * @see ScheduleActivityTaskDecisionAttributes#startToCloseTimeout
     */
    @Value.Default
    public String startToCloseTimeout() {
        return SWF_TIMEOUT_NONE;
    }

    /**
     * Sets the input for the {@link ActivityAction} that is translated to a
     * {@link ScheduleActivityTaskDecisionAttributes} if the Activity needs to be scheduled.
     *
     * @return this instance for fluent access
     * @see ScheduleActivityTaskDecisionAttributes#input
     */
    @Nullable
    public abstract Object[] input();

    /**
     * Set the task list for this activity.
     * If not set the activity will use its related workflow task list.
     * This allows for sending activity tasks to different lists.
     */
    public TaskListName taskList() {
        return DEFAULT_TASK_LIST;
    }

    /**
     * @see ScheduleActivityTaskDecisionAttributes#control
     */
    @Nullable
    public abstract String control();

    @Override
    public TaskType taskType() {
        return TaskType.ACTIVITY;
    }

    public CompletionStage<OutputType> decide(DecisionContext decisionContext) {
        EventState eventState = getState(decisionContext.events());
        Optional<Event> currentEvent = getCurrentEvent(decisionContext.events());
        switch (eventState) {
            case NOT_STARTED:
                decisionContext.addDecisions(createInitiateActivityDecision());
                break;
            case INITIAL:
                break;
            case ACTIVE:
                break;
            case RETRY:
                break;
            case SUCCESS:
                assert currentEvent.isPresent() : "If we are success, then the current event must be present";
                OutputType output = workflow().dataConverter().fromData(currentEvent.get().output(), outputType().getType());
                return CompletableFuture.completedFuture(output);
            case ERROR:
                assert currentEvent.isPresent() : "If we have error, then the current event must be present";
                Throwable failure = convertDetailsToThrowable(currentEvent.get());
                CompletableFuture<OutputType> failedFuture = new CompletableFuture<>();
                failedFuture.completeExceptionally(failure);
                return failedFuture;
            default:
                throw new IllegalStateException(format("%s unknown action state: %s", this, eventState));
        }
        return new CompletableFuture<>();
    }

    private Throwable convertDetailsToThrowable(Event event) {
        Throwable failure;
        try {
            failure = workflow().dataConverter().fromData(event.details(), Throwable.class);
        } catch (DataConverterException e) {
            failure = new RuntimeException(format("%s : %s", event.reason(), event.details()));
        }
        return failure;
    }

    /**
     * @return decision of type {@link DecisionType#ScheduleActivityTask}
     */
    public Decision createInitiateActivityDecision() {
        final String inputAsString = workflow().dataConverter().toData(input());
        return new Decision()
                .withDecisionType(DecisionType.ScheduleActivityTask)
                .withScheduleActivityTaskDecisionAttributes(new ScheduleActivityTaskDecisionAttributes()
                        .withActivityType(new ActivityType()
                                .withName(name().value())
                                .withVersion(version().value()))
                        .withActivityId(actionId().value())
                        .withTaskList(new TaskList()
                                .withName(taskList().value()))
                        .withInput(inputAsString)
                        .withControl(control())
                        .withHeartbeatTimeout(heartBeatTimeoutTimeout())
                        .withScheduleToCloseTimeout(scheduleToCloseTimeout())
                        .withScheduleToStartTimeout(scheduleToStartTimeout())
                        .withStartToCloseTimeout(startToCloseTimeout()));
    }

}

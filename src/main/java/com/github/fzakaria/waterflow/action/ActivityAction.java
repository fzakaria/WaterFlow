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
import com.google.common.reflect.TypeToken;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static com.github.fzakaria.waterflow.SwfConstants.SWF_TIMEOUT_NONE;
import static java.lang.String.format;

/**
 * The implementation of an SWF Activity.
 * An {@link OutputType} is needed to help the {@link com.github.fzakaria.waterflow.converter.DataConverter}
 * serialize/deserialize the output.
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(fluent = true)
public class ActivityAction<OutputType> extends Action<OutputType> {

    /**
     * The name for this Activity
     * @see com.github.fzakaria.waterflow.activity.ActivityMethod
     */
    private final String name;

    /**
     * The version for this Activity
     * @see com.github.fzakaria.waterflow.activity.ActivityMethod
     */
    private final String version;

    /**
     * Override activity's default heartbeat timeout.
     * Pass null unit or duration &lt;= 0 for a timeout of NONE.
     *
     * @see ScheduleActivityTaskDecisionAttributes#heartbeatTimeout
     */
    private String heartBeatTimeoutTimeout = SWF_TIMEOUT_NONE;

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
    private String scheduleToCloseTimeout = SWF_TIMEOUT_NONE;

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
    private String scheduleToStartTimeout = SWF_TIMEOUT_NONE;


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
    private String startToCloseTimeout = SWF_TIMEOUT_NONE;

    /**
     * @see ScheduleActivityTaskDecisionAttributes#input
     */
    private String input;

    /**
     * Set the task list for this activity.
     * If not set the activity will use its related workflow task list.
     * This allows for sending activity tasks to different lists.
     */
    private String taskList;

    /**
     * @see ScheduleActivityTaskDecisionAttributes#control
     */
    private String control;

    /**
     * Construct an action mapped to a registered SWF Activity.
     * Each SWF Activity task is identified by the combination of name and version.
     *
     * @param actionId workflow-unique identifier.
     * @param name registered name
     * @param version registered version
     */
    public ActivityAction(String actionId, String name, String version, Class<OutputType> outputType) {
        super(actionId, TypeToken.of(outputType));
        this.name = name;
        this.version = version;
    }

    public ActivityAction(String actionId, String name, String version, TypeToken<OutputType> outputType) {
        super(actionId, outputType);
        this.name = name;
        this.version = version;
    }


        /**
         * Sets the input for the {@link ActivityAction} that is translated to a
         * {@link ScheduleActivityTaskDecisionAttributes} if the Activity needs to be scheduled.
         * @param input The input to be serialized into a String
         * @return this instance for fluent access
         */
    public ActivityAction<OutputType> input(Object... input) {
        this.input = workflow.dataConverter().toData(input);
        return this;
    }

    @Override
    public TaskType taskType() {
        return TaskType.ACTIVITY;
    }

    public CompletionStage<OutputType> decide(Collection<Decision> decisions) {
        EventState eventState = getState();
        Optional<Event> currentEvent = getCurrentEvent();
        switch (eventState) {
            case NOT_STARTED:
                decisions.add(createInitiateActivityDecision());
                break;
            case INITIAL:
                break;
            case ACTIVE:
                break;
            case RETRY:
                break;
            case SUCCESS:
                assert currentEvent.isPresent() : "If we are success, then the current event must be present";
                OutputType output = workflow.dataConverter().fromData(currentEvent.get().output(), outputType.getType());
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
            failure = workflow.dataConverter().fromData(event.details(), Throwable.class);
        } catch (DataConverterException e) {
            failure = new RuntimeException(format("%s : %s", event.reason(), event.details()));
        }
        return failure;
    }

    /**
     * @return decision of type {@link DecisionType#ScheduleActivityTask}
     */
    public Decision createInitiateActivityDecision() {
        return new Decision()
                .withDecisionType(DecisionType.ScheduleActivityTask)
                .withScheduleActivityTaskDecisionAttributes(new ScheduleActivityTaskDecisionAttributes()
                        .withActivityType(new ActivityType()
                                .withName(name)
                                .withVersion(version))
                        .withActivityId(actionId())
                        .withTaskList(new TaskList()
                                .withName(taskList == null ? workflow.taskList() : taskList))
                        .withInput(input)
                        .withControl(control)
                        .withHeartbeatTimeout(heartBeatTimeoutTimeout)
                        .withScheduleToCloseTimeout(scheduleToCloseTimeout)
                        .withScheduleToStartTimeout(scheduleToStartTimeout)
                        .withStartToCloseTimeout(startToCloseTimeout));
    }

}

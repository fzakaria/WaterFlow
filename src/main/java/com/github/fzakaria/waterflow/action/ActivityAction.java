package com.github.fzakaria.waterflow.action;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.amazonaws.services.simpleworkflow.model.EventType;
import com.amazonaws.services.simpleworkflow.model.ScheduleActivityTaskDecisionAttributes;
import com.github.fzakaria.waterflow.TaskType;
import com.github.fzakaria.waterflow.event.Event;
import com.github.fzakaria.waterflow.event.EventState;
import com.github.fzakaria.waterflow.immutable.Control;
import com.github.fzakaria.waterflow.immutable.DecisionContext;
import com.github.fzakaria.waterflow.immutable.Name;
import com.github.fzakaria.waterflow.immutable.TaskListName;
import com.github.fzakaria.waterflow.immutable.Version;
import com.github.fzakaria.waterflow.retry.NoRetryStrategy;
import com.github.fzakaria.waterflow.retry.RetryStrategy;
import com.github.fzakaria.waterflow.swf.ScheduleActivityTaskDecisionBuilder;
import com.github.fzakaria.waterflow.swf.StartTimerDecisionBuilder;
import com.github.fzakaria.waterflow.swf.SwfConstants;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

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
     * @see SwfConstants#SWF_TIMEOUT_NONE
     */
    public abstract Optional<String> heartBeatTimeoutTimeout();

    /**
     * Override activity's default schedule to close timeout.
     * <pre>
     * schedule ---> start ---> close
     * |_____________________________|
     * </pre>
     * Pass null unit or duration &lt;= 0 for a timeout of NONE.
     *
     * @see ScheduleActivityTaskDecisionAttributes#scheduleToCloseTimeout
     * @see SwfConstants#SWF_TIMEOUT_NONE
     */
    public abstract Optional<String> scheduleToCloseTimeout();

    /**
     * Override activity's default schedule to close timeout.
     * <pre>
     * schedule ---> start ---> close
     * |_________________|
     * </pre>
     * Pass null unit or duration &lt;= 0 for a timeout of NONE.
     *
     * @see ScheduleActivityTaskDecisionAttributes#scheduleToStartTimeout
     * @see SwfConstants#SWF_TIMEOUT_NONE
     */
    public abstract Optional<String> scheduleToStartTimeout();


    /**
     * Override activity's default start to close timeout.
     * <pre>
     * schedule ---> start ---> close
     *              |_______________|
     * </pre>
     * Pass null unit or duration &lt;= 0 for a timeout of NONE.
     *
     * @see ScheduleActivityTaskDecisionAttributes#startToCloseTimeout
     * @see SwfConstants#SWF_TIMEOUT_NONE
     */
    public abstract Optional<String> startToCloseTimeout();

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
    public abstract Optional<TaskListName> taskList();

    /**
     * @see ScheduleActivityTaskDecisionAttributes#control
     */
    public abstract Optional<Control> control();

    /**
     * @see ScheduleActivityTaskDecisionAttributes#taskPriority
     */
    public abstract Optional<Integer> taskPriority();

    @Value.Default
    public RetryStrategy retryStrategy() {
        return NoRetryStrategy.INSTANCE;
    }

    @Override
    public TaskType taskType() {
        return TaskType.ACTIVITY;
    }

    @Override
    public CompletionStage<OutputType> decide(DecisionContext decisionContext) {
        EventState eventState = getState(decisionContext.events());
        Optional<Event> currentEvent = getCurrentEvent(decisionContext.events());
        switch (eventState) {
            case NOT_STARTED:
                decisionContext.addDecisions(createInitialDecision());
                break;
            case INITIAL:
                break;
            case ACTIVE:
                break;
            case RETRY:
                log.debug("retry, restart action");
                decisionContext.addDecisions(createInitialDecision());
                break;
            case SUCCESS:
                assert currentEvent.isPresent() : "If we are success, then the current event must be present";
                OutputType output = workflow().dataConverter().fromData(currentEvent.get().output(), outputType().getType());
                return CompletableFuture.completedFuture(output);
            case ERROR:
                assert currentEvent.isPresent() : "If we have error, then the current event must be present";
                long attempts = getEvents(decisionContext.events()).stream()
                        .filter(e -> e.type() == EventType.ActivityTaskFailed).count();

                Optional<Instant> firstStartTime = getEvents(decisionContext.events()).stream()
                        .filter(e -> e.type() == EventType.ActivityTaskStarted)
                        .reduce((a,b) -> b).map( Event::eventTimestamp);

                assert firstStartTime.isPresent() : "If we have error, then the firstStartTime event must be present";

                Duration timerDuration = retryStrategy().nextRetry(attempts,firstStartTime.get());
                if (timerDuration.isZero()) {
                    Throwable failure = convertDetailsToThrowable(currentEvent.get());
                    CompletableFuture<OutputType> failedFuture = new CompletableFuture<>();
                    failedFuture.completeExceptionally(failure);
                    return failedFuture;
                } else {
                    Control control = Control.of(format("Attempt #%s", attempts));
                    final Decision decision = StartTimerDecisionBuilder.builder().actionId(actionId())
                            .control(control).startToFireTimeout(timerDuration).build();
                    decisionContext.addDecisions(decision);
                    return new CompletableFuture<>();
                }
            default:
                throw new IllegalStateException(format("%s unknown action state: %s", this, eventState));
        }
        return new CompletableFuture<>();
    }

    private Decision createInitialDecision() {
        final Optional<String> input = Optional.ofNullable(input()).map(i -> workflow().dataConverter().toData(i));
        return  ScheduleActivityTaskDecisionBuilder
                .builder().actionId(actionId()).control(control()).heartbeatTimeout(heartBeatTimeoutTimeout())
                .input(input).name(name()).version(version()).scheduleToCloseTimeout(scheduleToCloseTimeout())
                .scheduleToStartTimeout(scheduleToStartTimeout())
                .taskListName(taskList()).taskPriority(taskPriority()).build();
    }

    /**
     * getState with support for Retries
     * @return current state for this action.
     * @see EventState for details on how state is calculated
     */
    @Override
    protected EventState getState(List<Event> events) {
        Optional<Event> currentEvent = getCurrentEvent(events);
        Optional<Event> timerEvent = currentEvent
                .filter(e -> e.type() == EventType.TimerFired || EventType.TimerCanceled == e.type());
        return timerEvent.map(t -> EventState.RETRY).orElse(super.getState(events));
    }

}

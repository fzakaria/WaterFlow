package com.github.fzakaria.waterflow.action;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.github.fzakaria.waterflow.TaskType;
import com.github.fzakaria.waterflow.event.Event;
import com.github.fzakaria.waterflow.event.EventState;
import com.github.fzakaria.waterflow.immutable.Control;
import com.github.fzakaria.waterflow.immutable.DecisionContext;
import com.github.fzakaria.waterflow.swf.StartTimerDecisionBuilder;
import com.google.common.reflect.TypeToken;
import org.immutables.value.Value;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static java.lang.String.format;

/**
 * Represents an SWF Timer.
 * A timer enables you to notify your decider when a certain amount of time has elapsed.
 * One common application for a timer is to delay the execution of an activity task.
 * For example, a customer might want to take delayed delivery of an item.
 */
@Value.Immutable
public abstract class TimerAction extends Action<Void> {

    @Override
    public TaskType taskType() {
        return TaskType.TIMER;
    }

    @Override
    public TypeToken<Void> outputType() {
        return TypeToken.of(Void.class);
    }

    /**
     * The details for this marker
     */
    public abstract Optional<Control> control();

    /**
     * The duration to wait before firing the timer.
     */
    public abstract Duration startToFireTimeout();

    @Override
    public CompletionStage<Void> decide(DecisionContext decisionContext) {
        EventState eventState = getState(decisionContext.events());
        Optional<Event> currentEvent = getCurrentEvent(decisionContext.events());
        switch (eventState) {
            case NOT_STARTED:
                final Decision decision = StartTimerDecisionBuilder.builder().actionId(actionId())
                        .control(control()).startToFireTimeout(startToFireTimeout()).build();
                decisionContext.addDecisions(decision);
                break;
            case INITIAL:
                break;
            case ACTIVE:
                break;
            case RETRY:
                break;
            case SUCCESS:
                return CompletableFuture.completedFuture(null);
            case ERROR:
                assert currentEvent.isPresent() : "If we have error, then the current event must be present";
                log.warn("StartTimer has failed - {}", currentEvent.get().details());
                Throwable failure = new IllegalStateException(currentEvent.get().details());
                CompletableFuture<Void> failedFuture = new CompletableFuture<>();
                failedFuture.completeExceptionally(failure);
                return failedFuture;
            default:
                throw new IllegalStateException(format("%s unknown action state: %s", this, eventState));
        }
        return new CompletableFuture<>();
    }

}

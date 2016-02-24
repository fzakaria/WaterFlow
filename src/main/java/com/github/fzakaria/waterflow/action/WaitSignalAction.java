package com.github.fzakaria.waterflow.action;

import com.github.fzakaria.waterflow.TaskType;
import com.github.fzakaria.waterflow.event.Event;
import com.github.fzakaria.waterflow.event.EventState;
import com.github.fzakaria.waterflow.immutable.DecisionContext;
import com.google.common.reflect.TypeToken;
import org.immutables.value.Value;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static java.lang.String.format;

/**
 * Represents the ability to wait for an external signal.
 * Signals can be sent to a workflow from external actors (other workflows, or through the API)
 * and are used usually to represent human activity tasks.
 */
@Value.Immutable
public abstract class WaitSignalAction extends Action<String>  {

    @Override
    public TaskType taskType() {
        return TaskType.WORKFLOW_SIGNALED;
    }

    @Override
    public TypeToken<String> outputType() {
        return TypeToken.of(String.class);
    }


    @Override
    public CompletionStage<String> decide(DecisionContext decisionContext) {
        EventState eventState = getState(decisionContext.events());
        Optional<Event> currentEvent = getCurrentEvent(decisionContext.events());
        switch (eventState) {
            case NOT_STARTED:
                break;
            case INITIAL:
                break;
            case ACTIVE:
                break;
            case RETRY:
                break;
            case SUCCESS:
                assert currentEvent.isPresent() : "If we are success, then the current event must be present";
                String output = workflow().dataConverter().fromData(currentEvent.get().output(), outputType().getType());
                return CompletableFuture.completedFuture(output);
            default:
                throw new IllegalStateException(format("%s unknown action state: %s", this, eventState));
        }
        return new CompletableFuture<>();
    }
}

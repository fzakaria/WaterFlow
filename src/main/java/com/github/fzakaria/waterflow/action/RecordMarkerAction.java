package com.github.fzakaria.waterflow.action;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.github.fzakaria.waterflow.TaskType;
import com.github.fzakaria.waterflow.event.Event;
import com.github.fzakaria.waterflow.event.EventState;
import com.github.fzakaria.waterflow.immutable.DecisionContext;
import com.github.fzakaria.waterflow.immutable.Details;
import com.github.fzakaria.waterflow.swf.RecordMarkerDecisionBuilder;
import com.google.common.reflect.TypeToken;
import org.immutables.value.Value;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static java.lang.String.format;

/**
 * Add a marker to a SWF workflow.
 * <p/>
 * <b>WARNING</b>: Marker actions do not cause an additional decision event to be issued by SWF.
 * <p/>
 * <p>
 * In order to make sure another decision is kicked off, the future returned is immediately
 * completed so that further decisions can be added to the list of decisions.
 * <p/>
 *
 */
@Value.Immutable
public abstract class RecordMarkerAction extends Action<Void> {

    /**
     * The details for this marker
     */
    public abstract Optional<Details> details();

    @Override
    public TaskType taskType() {
        return TaskType.RECORD_MARKER;
    }

    @Override
    public TypeToken<Void> outputType() {
        return TypeToken.of(Void.class);
    }

    @Override
    public CompletionStage<Void> decide(DecisionContext decisionContext) {
        EventState eventState = getState(decisionContext.events());
        Optional<Event> currentEvent = getCurrentEvent(decisionContext.events());
        switch (eventState) {
            case NOT_STARTED:
                Decision decision =
                        RecordMarkerDecisionBuilder.builder().actionId(actionId()).details(details()).build();
                decisionContext.addDecisions(decision);
                break;
            case INITIAL:
                break;
            case ACTIVE:
                break;
            case RETRY:
                break;
            case SUCCESS:
                break;
            case ERROR:
                assert currentEvent.isPresent() : "If we have error, then the current event must be present";
                log.error("The ReordMarkerDecision failed. The only cause is that the the operation is not permitted.");
                throw new IllegalAccessError("Operation not permitted.");
            default:
                throw new IllegalStateException(format("%s unknown action state: %s", this, eventState));
        }
        return CompletableFuture.completedFuture(null);
    }



}

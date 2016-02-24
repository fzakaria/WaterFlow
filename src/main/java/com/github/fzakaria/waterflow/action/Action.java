package com.github.fzakaria.waterflow.action;

import com.github.fzakaria.waterflow.TaskType;
import com.github.fzakaria.waterflow.Workflow;
import com.github.fzakaria.waterflow.converter.DataConverterException;
import com.github.fzakaria.waterflow.event.Event;
import com.github.fzakaria.waterflow.event.EventState;
import com.github.fzakaria.waterflow.immutable.ActionId;
import com.github.fzakaria.waterflow.immutable.DecisionContext;
import com.google.common.base.Preconditions;
import com.google.common.reflect.TypeToken;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import static com.github.fzakaria.waterflow.event.EventState.NOT_STARTED;
import static java.lang.String.format;

/**
 * Combines the concepts of SWF Activities, Signals, Child Workflow, and Timers and their current running state.
 * <p/>
 * Note: The name "Action" was chosen to avoid naming conflicts with the parallel SWF concept "Task".
 * Concept heavily based from <a href="https://bitbucket.org/clarioanalytics/services-swift">services-swift</a>
 */
public abstract class Action<OutputType>  {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * The unique id of the action for the workflow.
     */
    public abstract ActionId actionId();

    /**
     * The output of the action's class.
     * If the Action doesn't emit a result, use {@link Void}
     */
    public abstract TypeToken<OutputType> outputType();

    /**
     * The workflow with which this action belongs to.
     */
    public abstract Workflow<?,?> workflow();

    /**
     * Get the category of task this action belongs to.
     * Subclasses should fill this in.
     * @return The {@link TaskType} this {@link Action} is
     */
    public abstract TaskType taskType();


    /**
     * Subclasses must implement this to decide how to handle the action.
     * Care must be taken care to review history so that the same action is not executed multiple
     * times during workflow replay.
     */
    public abstract CompletionStage<OutputType> decide(DecisionContext decisionContext);

    @Value.Check
    protected void check() {
        Preconditions.checkNotNull(workflow(),
                "This action must be given a workflow.");
    }

    /**
     * Get the most recent event for this {@link Action}.
     * Events are filtered by {@link #actionId}
     */
    protected Optional<Event> getCurrentEvent(List<Event> events) {
        return getEvents(events).stream().findFirst();
    }

    /**
     * Events in reverse chronological order
     * @return Workflow {@link Event} selected by {@link #actionId()} && {@link #taskType()} ()}
     */
    protected List<Event> getTaskEvents(List<Event> events) {
        return getEvents(events).stream().filter(e -> e.task() == taskType()).collect(Collectors.toList());
    }

    /**
     * @return current state for this action.
     * @see EventState for details on how state is calculated
     */
    protected EventState getState(List<Event> events) {
        Optional<Event> currentEvent = getCurrentEvent(events);
        return currentEvent.map(Event::state).orElse(NOT_STARTED);
    }

    /**
     * Returns the events filtered by {@link #actionId()}
     */
    protected List<Event> getEvents(List<Event> events) {
        return events.stream().filter(e -> Objects.equals(e.actionId(), actionId())).collect(Collectors.toList());
    }


    protected Throwable convertDetailsToThrowable(Event event) {
        Throwable failure;
        try {
            failure = workflow().dataConverter().fromData(event.details(), Throwable.class);
        } catch (DataConverterException e) {
            failure = new RuntimeException(format("%s : %s", event.reason(), event.details()));
        }
        return failure;
    }
}

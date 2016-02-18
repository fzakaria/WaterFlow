package com.github.fzakaria.waterflow.action;

import com.github.fzakaria.waterflow.TaskType;
import com.github.fzakaria.waterflow.Workflow;
import com.github.fzakaria.waterflow.event.Event;
import com.github.fzakaria.waterflow.event.EventState;
import com.google.common.reflect.TypeToken;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.github.fzakaria.waterflow.event.EventState.NOT_STARTED;
import static java.lang.String.format;

/**
 * Combines the concepts of SWF Activities, Signals, Child Workflows, and Timers and their current running state.
 * <p/>
 * Note: The name "Action" was chosen to avoid naming conflicts with the parallel SWF concept "Task".
 * Concept heavily based from <a href="https://bitbucket.org/clarioanalytics/services-swift">services-swift</a>
 */
@Slf4j
@Data
@Accessors(fluent = true)
public abstract class Action<OutputType>  {

    /**
     * The unique id of the action for the workflow.
     */
    protected final String actionId;

    /**
     * The output of the action's class.
     * If the Action doesn't emit a result, use {@link Void}
     */
    protected final TypeToken<OutputType> outputType;

    /**
     * The workflow with which this action belongs to.
     * This is usually set in {@link Workflow#actions(Action...)}
     */
    protected Workflow workflow;

    /**
     * Get the category of task this action belongs to.
     * Subclasses should fill this in.
     * @return The {@link TaskType} this {@link Action} is
     */
    public abstract TaskType taskType();

    /**
     * Get the most recent event for this {@link Action}.
     * Events are filtered by {@link #actionId}
     */
    protected Optional<Event> getCurrentEvent() {
        return getEvents().stream().findFirst();
    }

    /**
     * Events in reverse chronological order (most recent first)
     * @return Workflow {@link Event} selected by {@link #actionId}
     */
    protected List<Event> getEvents() {
        assertWorkflowSet();
        List<Event> allEvents = workflow.events();
        return allEvents.stream().filter(e -> Objects.equals(e.actionId(), actionId)).collect(Collectors.toList());
    }

    /**
     * Events in reverse chronological order
     * @return Workflow {@link Event} selected by {@link #actionId()} && {@link #taskType()} ()}
     */
    protected List<Event> getTaskEvents() {
        return getEvents().stream().filter(e -> e.task() == taskType()).collect(Collectors.toList());
    }

    /**
     * @return current state for this action.
     * @see EventState for details on how state is calculated
     */
    protected EventState getState() {
        Optional<Event> currentEvent = getCurrentEvent();
        return currentEvent.map(Event::state).orElse(NOT_STARTED);
    }

    protected void assertWorkflowSet() {
        if (workflow == null) {
            throw new IllegalStateException(format("%s has no associated workflow. Ensure all actions used by a workflow are added to the workflow.", toString()));
        }
    }
}

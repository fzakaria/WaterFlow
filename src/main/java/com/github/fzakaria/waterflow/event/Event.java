package com.github.fzakaria.waterflow.event;


import com.amazonaws.services.simpleworkflow.model.EventType;
import com.amazonaws.services.simpleworkflow.model.HistoryEvent;
import com.github.fzakaria.waterflow.TaskType;
import com.github.fzakaria.waterflow.immutable.ActionId;
import org.immutables.value.Value;
import org.joda.time.DateTime;

import java.util.List;
import java.util.Optional;

import static com.github.fzakaria.waterflow.event.EventState.*;
import static com.github.fzakaria.waterflow.TaskType.*;
import static com.amazonaws.services.simpleworkflow.model.EventType.*;
import static java.lang.String.format;


/**
 * {@link Event} class consolidates SWF {@link HistoryEvent} types
 * so groups of similar event types can be accessed in a uniform way.
 */
@Value.Immutable
public abstract class Event implements Comparable<Event> {

    public abstract HistoryEvent historyEvent();

    /**
     * All history events are required since we need to find the previous events
     * they might refer to.
     */
    public abstract List<HistoryEvent> historyEvents();

    public EventType type() { return EventType.valueOf(historyEvent().getEventType()); }

    public Long id() { return historyEvent().getEventId(); }

    public DateTime eventTimestamp() { return new DateTime(historyEvent().getEventTimestamp()); }

    public TaskType task() {
        if (WorkflowExecutionStarted == type()) { return WORKFLOW_EXECUTION; }
        if (WorkflowExecutionCancelRequested == type()) { return WORKFLOW_EXECUTION; }
        if (WorkflowExecutionCompleted == type()) { return WORKFLOW_EXECUTION; }
        if (CompleteWorkflowExecutionFailed == type()) { return WORKFLOW_EXECUTION; }
        if (WorkflowExecutionFailed == type()) { return WORKFLOW_EXECUTION; }
        if (FailWorkflowExecutionFailed == type()) { return WORKFLOW_EXECUTION; }
        if (WorkflowExecutionTimedOut == type()) { return WORKFLOW_EXECUTION; }
        if (WorkflowExecutionCanceled == type()) { return WORKFLOW_EXECUTION; }
        if (CancelWorkflowExecutionFailed == type()) { return WORKFLOW_EXECUTION; }
        if (WorkflowExecutionContinuedAsNew == type()) { return CONTINUE_AS_NEW; }
        if (ContinueAsNewWorkflowExecutionFailed == type()) { return CONTINUE_AS_NEW; }
        if (WorkflowExecutionTerminated == type()) { return WORKFLOW_EXECUTION; }
        if (DecisionTaskScheduled == type()) { return DECISION; }
        if (DecisionTaskStarted == type()) { return DECISION; }
        if (DecisionTaskCompleted == type()) { return DECISION; }
        if (DecisionTaskTimedOut == type()) { return DECISION; }
        if (ActivityTaskScheduled == type()) { return ACTIVITY; }
        if (ScheduleActivityTaskFailed == type()) { return ACTIVITY; }
        if (ActivityTaskStarted == type()) { return ACTIVITY; }
        if (ActivityTaskCompleted == type()) { return ACTIVITY; }
        if (ActivityTaskFailed == type()) { return ACTIVITY; }
        if (ActivityTaskTimedOut == type()) { return ACTIVITY; }
        if (ActivityTaskCanceled == type()) { return ACTIVITY; }
        if (ActivityTaskCancelRequested == type()) { return ACTIVITY; }
        if (RequestCancelActivityTaskFailed == type()) { return ACTIVITY; }
        if (WorkflowExecutionSignaled == type()) { return WORKFLOW_SIGNALED; }
        if (MarkerRecorded == type()) { return RECORD_MARKER; }
        if (RecordMarkerFailed == type()) { return RECORD_MARKER; }
        if (TimerStarted == type()) { return TIMER; }
        if (StartTimerFailed == type()) { return TIMER; }
        if (TimerFired == type()) { return TIMER; }
        if (TimerCanceled == type()) { return TIMER; }
        if (CancelTimerFailed == type()) { return TIMER; }
        if (StartChildWorkflowExecutionInitiated == type()) { return START_CHILD_WORKFLOW; }
        if (StartChildWorkflowExecutionFailed == type()) { return START_CHILD_WORKFLOW; }
        if (ChildWorkflowExecutionStarted == type()) { return START_CHILD_WORKFLOW; }
        if (ChildWorkflowExecutionCompleted == type()) { return START_CHILD_WORKFLOW; }
        if (ChildWorkflowExecutionFailed == type()) { return START_CHILD_WORKFLOW; }
        if (ChildWorkflowExecutionTimedOut == type()) { return START_CHILD_WORKFLOW; }
        if (ChildWorkflowExecutionCanceled == type()) { return START_CHILD_WORKFLOW; }
        if (ChildWorkflowExecutionTerminated == type()) { return START_CHILD_WORKFLOW; }
        if (SignalExternalWorkflowExecutionInitiated == type()) { return SIGNAL_EXTERNAL_WORKFLOW; }
        if (SignalExternalWorkflowExecutionFailed == type()) { return SIGNAL_EXTERNAL_WORKFLOW; }
        if (ExternalWorkflowExecutionSignaled == type()) { return SIGNAL_EXTERNAL_WORKFLOW; }
        if (RequestCancelExternalWorkflowExecutionInitiated == type()) { return CANCEL_EXTERNAL_WORKFLOW; }
        if (RequestCancelExternalWorkflowExecutionFailed == type()) { return CANCEL_EXTERNAL_WORKFLOW; }
        if (ExternalWorkflowExecutionCancelRequested == type()) { return CANCEL_EXTERNAL_WORKFLOW; }
        throw new IllegalArgumentException("Unknown EventType " + type());
    }

    public EventState state() {
        if (WorkflowExecutionStarted == type()) { return INITIAL; }
        if (WorkflowExecutionCancelRequested == type()) { return ACTIVE; }
        if (WorkflowExecutionCompleted == type()) { return SUCCESS; }
        if (CompleteWorkflowExecutionFailed == type()) { return ERROR; }
        if (WorkflowExecutionFailed == type()) { return ERROR; }
        if (FailWorkflowExecutionFailed == type()) { return ERROR; }
        if (WorkflowExecutionTimedOut == type()) { return ERROR; }
        if (WorkflowExecutionCanceled == type()) { return ERROR; }
        if (CancelWorkflowExecutionFailed == type()) { return ERROR; }
        if (WorkflowExecutionContinuedAsNew == type()) { return INITIAL; }
        if (ContinueAsNewWorkflowExecutionFailed == type()) { return ERROR; }
        if (WorkflowExecutionTerminated == type()) { return ERROR; }
        if (DecisionTaskScheduled == type()) { return INITIAL; }
        if (DecisionTaskStarted == type()) { return ACTIVE; }
        if (DecisionTaskCompleted == type()) { return SUCCESS; }
        if (DecisionTaskTimedOut == type()) { return ERROR; }
        if (ActivityTaskScheduled == type()) { return INITIAL; }
        if (ScheduleActivityTaskFailed == type()) { return ERROR; }
        if (ActivityTaskStarted == type()) { return ACTIVE; }
        if (ActivityTaskCompleted == type()) { return SUCCESS; }
        if (ActivityTaskFailed == type()) { return ERROR; }
        if (ActivityTaskTimedOut == type()) { return ERROR; }
        if (ActivityTaskCanceled == type()) { return ERROR; }
        if (ActivityTaskCancelRequested == type()) { return ERROR; }
        if (RequestCancelActivityTaskFailed == type()) { return ERROR; }
        if (WorkflowExecutionSignaled == type()) { return SUCCESS; }
        if (MarkerRecorded == type()) { return INITIAL; }
        if (RecordMarkerFailed == type()) { return ERROR; }
        if (TimerStarted == type()) { return INITIAL; }
        if (StartTimerFailed == type()) { return ERROR; }
        if (TimerFired == type()) { return SUCCESS; }
        if (TimerCanceled == type()) { return SUCCESS; }
        if (CancelTimerFailed == type()) { return ACTIVE; }
        if (StartChildWorkflowExecutionInitiated == type()) { return INITIAL; }
        if (StartChildWorkflowExecutionFailed == type()) { return ERROR; }
        if (ChildWorkflowExecutionStarted == type()) { return ACTIVE; }
        if (ChildWorkflowExecutionCompleted == type()) { return SUCCESS; }
        if (ChildWorkflowExecutionFailed == type()) { return ERROR; }
        if (ChildWorkflowExecutionTimedOut == type()) { return ERROR; }
        if (ChildWorkflowExecutionCanceled == type()) { return ERROR; }
        if (ChildWorkflowExecutionTerminated == type()) { return ERROR; }
        if (SignalExternalWorkflowExecutionInitiated == type()) { return INITIAL; }
        if (SignalExternalWorkflowExecutionFailed == type()) { return ERROR; }
        if (ExternalWorkflowExecutionSignaled == type()) { return SUCCESS; }
        if (RequestCancelExternalWorkflowExecutionInitiated == type()) { return INITIAL; }
        if (RequestCancelExternalWorkflowExecutionFailed == type()) { return ERROR; }
        if (ExternalWorkflowExecutionCancelRequested == type()) { return SUCCESS; }
        throw new IllegalArgumentException("Unknown EventType " + type());
    }

    public Long initialEventId() {
        if (WorkflowExecutionStarted == type()) { return historyEvent().getEventId(); }
        if (WorkflowExecutionCancelRequested == type()) { return historyEvent().getEventId(); }
        if (WorkflowExecutionCompleted == type()) { return historyEvent().getEventId(); }
        if (CompleteWorkflowExecutionFailed == type()) { return historyEvent().getEventId(); }
        if (WorkflowExecutionFailed == type()) { return historyEvent().getEventId(); }
        if (FailWorkflowExecutionFailed == type()) { return historyEvent().getEventId(); }
        if (WorkflowExecutionTimedOut == type()) { return historyEvent().getEventId(); }
        if (WorkflowExecutionCanceled == type()) { return historyEvent().getEventId(); }
        if (CancelWorkflowExecutionFailed == type()) { return historyEvent().getEventId(); }
        if (WorkflowExecutionContinuedAsNew == type()) { return historyEvent().getEventId(); }
        if (ContinueAsNewWorkflowExecutionFailed == type()) { return historyEvent().getEventId(); }
        if (WorkflowExecutionTerminated == type()) { return historyEvent().getEventId(); }
        if (DecisionTaskScheduled == type()) { return historyEvent().getEventId(); }
        if (DecisionTaskStarted == type()) { return historyEvent().getEventId(); }
        if (DecisionTaskCompleted == type()) { return historyEvent().getDecisionTaskCompletedEventAttributes().getScheduledEventId(); }
        if (DecisionTaskTimedOut == type()) { return historyEvent().getEventId(); }
        if (ActivityTaskScheduled == type()) { return historyEvent().getEventId(); }
        if (ScheduleActivityTaskFailed == type()) { return historyEvent().getEventId(); }
        if (ActivityTaskStarted == type()) { return historyEvent().getActivityTaskStartedEventAttributes().getScheduledEventId(); }
        if (ActivityTaskCompleted == type()) { return historyEvent().getActivityTaskCompletedEventAttributes().getScheduledEventId(); }
        if (ActivityTaskFailed == type()) { return historyEvent().getActivityTaskFailedEventAttributes().getScheduledEventId(); }
        if (ActivityTaskTimedOut == type()) { return historyEvent().getActivityTaskTimedOutEventAttributes().getScheduledEventId(); }
        if (ActivityTaskCanceled == type()) { return historyEvent().getActivityTaskCanceledEventAttributes().getScheduledEventId(); }
        if (ActivityTaskCancelRequested == type()) { return historyEvent().getEventId(); }
        if (RequestCancelActivityTaskFailed == type()) { return historyEvent().getEventId(); }
        if (WorkflowExecutionSignaled == type()) { return historyEvent().getEventId(); }
        if (MarkerRecorded == type()) { return historyEvent().getEventId(); }
        if (RecordMarkerFailed == type()) { return historyEvent().getEventId(); }
        if (TimerStarted == type()) { return historyEvent().getEventId(); }
        if (StartTimerFailed == type()) { return null; }
        if (TimerFired == type()) { return historyEvent().getTimerFiredEventAttributes().getStartedEventId(); }
        if (TimerCanceled == type()) { return historyEvent().getTimerCanceledEventAttributes().getStartedEventId(); }
        if (CancelTimerFailed == type()) { return historyEvent().getEventId(); }
        if (StartChildWorkflowExecutionInitiated == type()) { return historyEvent().getEventId(); }
        if (StartChildWorkflowExecutionFailed == type()) { return historyEvent().getStartChildWorkflowExecutionFailedEventAttributes().getInitiatedEventId(); }
        if (ChildWorkflowExecutionStarted == type()) { return historyEvent().getChildWorkflowExecutionStartedEventAttributes().getInitiatedEventId(); }
        if (ChildWorkflowExecutionCompleted == type()) { return historyEvent().getChildWorkflowExecutionCompletedEventAttributes().getInitiatedEventId(); }
        if (ChildWorkflowExecutionFailed == type()) { return historyEvent().getChildWorkflowExecutionFailedEventAttributes().getInitiatedEventId(); }
        if (ChildWorkflowExecutionTimedOut == type()) { return historyEvent().getChildWorkflowExecutionTimedOutEventAttributes().getInitiatedEventId(); }
        if (ChildWorkflowExecutionCanceled == type()) { return historyEvent().getChildWorkflowExecutionCanceledEventAttributes().getInitiatedEventId(); }
        if (ChildWorkflowExecutionTerminated == type()) { return historyEvent().getChildWorkflowExecutionTerminatedEventAttributes().getInitiatedEventId(); }
        if (SignalExternalWorkflowExecutionInitiated == type()) { return historyEvent().getEventId(); }
        if (SignalExternalWorkflowExecutionFailed == type()) { return historyEvent().getSignalExternalWorkflowExecutionFailedEventAttributes().getInitiatedEventId(); }
        if (ExternalWorkflowExecutionSignaled == type()) { return historyEvent().getExternalWorkflowExecutionSignaledEventAttributes().getInitiatedEventId(); }
        if (RequestCancelExternalWorkflowExecutionInitiated == type()) { return historyEvent().getEventId(); }
        if (RequestCancelExternalWorkflowExecutionFailed == type()) { return historyEvent().getEventId(); }
        if (ExternalWorkflowExecutionCancelRequested == type()) { return historyEvent().getEventId(); }
        throw new IllegalArgumentException("Unknown EventType " + type());
    }

    public ActionId actionId() {
        if (WorkflowExecutionStarted == type()) { return null; }
        if (WorkflowExecutionCancelRequested == type()) { return null; }
        if (WorkflowExecutionCompleted == type()) { return null; }
        if (CompleteWorkflowExecutionFailed == type()) { return null; }
        if (WorkflowExecutionFailed == type()) { return null; }
        if (FailWorkflowExecutionFailed == type()) { return null; }
        if (WorkflowExecutionTimedOut == type()) { return null; }
        if (WorkflowExecutionCanceled == type()) { return null; }
        if (CancelWorkflowExecutionFailed == type()) { return null; }
        if (WorkflowExecutionContinuedAsNew == type()) { return null; }
        if (ContinueAsNewWorkflowExecutionFailed == type()) { return null; }
        if (WorkflowExecutionTerminated == type()) { return null; }
        if (DecisionTaskScheduled == type()) { return null; }
        if (DecisionTaskStarted == type()) { return null; }
        if (DecisionTaskCompleted == type()) { return null; }
        if (DecisionTaskTimedOut == type()) { return null; }
        if (ActivityTaskScheduled == type()) { return ActionId.of(historyEvent().getActivityTaskScheduledEventAttributes().getActivityId()); }
        if (ScheduleActivityTaskFailed == type()) { return ActionId.of(historyEvent().getScheduleActivityTaskFailedEventAttributes().getActivityId()); }
        if (ActivityTaskStarted == type()) {
            long scheduledEventId = historyEvent().getActivityTaskStartedEventAttributes().getScheduledEventId();
            Optional<HistoryEvent> scheduledEvent = historyEvents().stream().filter(he -> he.getEventId() == scheduledEventId).findFirst();
            assert scheduledEvent.isPresent() : "If we have task started then there must be scheduled event";
            return ImmutableEvent.builder().from(this).historyEvent(scheduledEvent.get()).build().actionId();
        }
        if (ActivityTaskCompleted == type()) {
            long scheduledEventId = historyEvent().getActivityTaskCompletedEventAttributes().getScheduledEventId();
            Optional<HistoryEvent> scheduledEvent = historyEvents().stream().filter(he -> he.getEventId() == scheduledEventId).findFirst();
            assert scheduledEvent.isPresent() : "If we have task completed then there must be scheduled event";
            return ImmutableEvent.builder().from(this).historyEvent(scheduledEvent.get()).build().actionId();
        }
        if (ActivityTaskFailed == type()) {
            long scheduledEventId = historyEvent().getActivityTaskFailedEventAttributes().getScheduledEventId();
            Optional<HistoryEvent> scheduledEvent = historyEvents().stream().filter(he -> he.getEventId() == scheduledEventId).findFirst();
            assert scheduledEvent.isPresent() : "If we have task failed then there must be scheduled event";
            return ImmutableEvent.builder().from(this).historyEvent(scheduledEvent.get()).build().actionId();
        }
        if (ActivityTaskTimedOut == type()) {
            long scheduledEventId = historyEvent().getActivityTaskTimedOutEventAttributes().getScheduledEventId();
            Optional<HistoryEvent> scheduledEvent = historyEvents().stream().filter(he -> he.getEventId() == scheduledEventId).findFirst();
            assert scheduledEvent.isPresent() : "If we have task timeout then there must be scheduled event";
            return ImmutableEvent.builder().from(this).historyEvent(scheduledEvent.get()).build().actionId();
        }
        if (ActivityTaskCanceled == type()) {
            long scheduledEventId = historyEvent().getActivityTaskCanceledEventAttributes().getScheduledEventId();
            Optional<HistoryEvent> scheduledEvent = historyEvents().stream().filter(he -> he.getEventId() == scheduledEventId).findFirst();
            assert scheduledEvent.isPresent() : "If we have task cancelled then there must be scheduled event";
            return ImmutableEvent.builder().from(this).historyEvent(scheduledEvent.get()).build().actionId();
        }
        if (ActivityTaskCancelRequested == type()) { return null; }
        if (RequestCancelActivityTaskFailed == type()) { return null; }
        if (WorkflowExecutionSignaled == type()) { return ActionId.of(historyEvent().getWorkflowExecutionSignaledEventAttributes().getSignalName()); }
        if (MarkerRecorded == type()) { return ActionId.of(historyEvent().getMarkerRecordedEventAttributes().getMarkerName()); }
        if (RecordMarkerFailed == type()) { return null; }
        if (TimerStarted == type()) { return ActionId.of(historyEvent().getTimerStartedEventAttributes().getTimerId()); }
        if (StartTimerFailed == type()) { return ActionId.of(historyEvent().getStartTimerFailedEventAttributes().getTimerId()); }
        if (TimerFired == type()) { return ActionId.of(historyEvent().getTimerFiredEventAttributes().getTimerId()); }
        if (TimerCanceled == type()) { return ActionId.of(historyEvent().getTimerCanceledEventAttributes().getTimerId()); }
        if (CancelTimerFailed == type()) { return ActionId.of(historyEvent().getCancelTimerFailedEventAttributes().getTimerId()); }
        if (StartChildWorkflowExecutionInitiated == type()) { return ActionId.of(historyEvent().getStartChildWorkflowExecutionInitiatedEventAttributes().getControl()); }
        if (StartChildWorkflowExecutionFailed == type()) { return null; }
        if (ChildWorkflowExecutionStarted == type()) { return null; }
        if (ChildWorkflowExecutionCompleted == type()) { return null; }
        if (ChildWorkflowExecutionFailed == type()) { return null; }
        if (ChildWorkflowExecutionTimedOut == type()) { return null; }
        if (ChildWorkflowExecutionCanceled == type()) { return null; }
        if (ChildWorkflowExecutionTerminated == type()) { return null; }
        if (SignalExternalWorkflowExecutionInitiated == type()) { return ActionId.of(historyEvent().getSignalExternalWorkflowExecutionInitiatedEventAttributes().getSignalName()); }
        if (SignalExternalWorkflowExecutionFailed == type()) { return null; }
        if (ExternalWorkflowExecutionSignaled == type()) { return null; }
        if (RequestCancelExternalWorkflowExecutionInitiated == type()) { return ActionId.of(historyEvent().getRequestCancelExternalWorkflowExecutionInitiatedEventAttributes().getControl()); }
        if (RequestCancelExternalWorkflowExecutionFailed == type()) { return ActionId.of(historyEvent().getRequestCancelExternalWorkflowExecutionFailedEventAttributes().getControl()); }
        if (ExternalWorkflowExecutionCancelRequested == type()) { return null; }
        throw new IllegalArgumentException("Unknown EventType " + type());
    }

    public String input() {
        if (WorkflowExecutionStarted == type()) { return historyEvent().getWorkflowExecutionStartedEventAttributes().getInput(); }
        if (WorkflowExecutionCancelRequested == type()) { return null; }
        if (WorkflowExecutionCompleted == type()) { return null; }
        if (CompleteWorkflowExecutionFailed == type()) { return null; }
        if (WorkflowExecutionFailed == type()) { return null; }
        if (FailWorkflowExecutionFailed == type()) { return null; }
        if (WorkflowExecutionTimedOut == type()) { return null; }
        if (WorkflowExecutionCanceled == type()) { return null; }
        if (CancelWorkflowExecutionFailed == type()) { return null; }
        if (WorkflowExecutionContinuedAsNew == type()) { return historyEvent().getWorkflowExecutionContinuedAsNewEventAttributes().getInput(); }
        if (ContinueAsNewWorkflowExecutionFailed == type()) { return null; }
        if (WorkflowExecutionTerminated == type()) { return null; }
        if (DecisionTaskScheduled == type()) { return null; }
        if (DecisionTaskStarted == type()) { return null; }
        if (DecisionTaskCompleted == type()) { return null; }
        if (DecisionTaskTimedOut == type()) { return null; }
        if (ActivityTaskScheduled == type()) { return historyEvent().getActivityTaskScheduledEventAttributes().getInput(); }
        if (ScheduleActivityTaskFailed == type()) { return null; }
        if (ActivityTaskStarted == type()) { return null; }
        if (ActivityTaskCompleted == type()) { return null; }
        if (ActivityTaskFailed == type()) { return null; }
        if (ActivityTaskTimedOut == type()) { return null; }
        if (ActivityTaskCanceled == type()) { return null; }
        if (ActivityTaskCancelRequested == type()) { return null; }
        if (RequestCancelActivityTaskFailed == type()) { return null; }
        if (WorkflowExecutionSignaled == type()) { return historyEvent().getWorkflowExecutionSignaledEventAttributes().getInput(); }
        if (MarkerRecorded == type()) { return historyEvent().getMarkerRecordedEventAttributes().getDetails(); }
        if (RecordMarkerFailed == type()) { return null; }
        if (TimerStarted == type()) { return "Timer Started"; }
        if (StartTimerFailed == type()) { return null; }
        if (TimerFired == type()) { return null; }
        if (TimerCanceled == type()) { return null; }
        if (CancelTimerFailed == type()) { return null; }
        if (StartChildWorkflowExecutionInitiated == type()) { return historyEvent().getStartChildWorkflowExecutionInitiatedEventAttributes().getInput(); }
        if (StartChildWorkflowExecutionFailed == type()) { return null; }
        if (ChildWorkflowExecutionStarted == type()) { return null; }
        if (ChildWorkflowExecutionCompleted == type()) { return null; }
        if (ChildWorkflowExecutionFailed == type()) { return null; }
        if (ChildWorkflowExecutionTimedOut == type()) { return null; }
        if (ChildWorkflowExecutionCanceled == type()) { return null; }
        if (ChildWorkflowExecutionTerminated == type()) { return null; }
        if (SignalExternalWorkflowExecutionInitiated == type()) { return historyEvent().getSignalExternalWorkflowExecutionInitiatedEventAttributes().getInput(); }
        if (SignalExternalWorkflowExecutionFailed == type()) { return null; }
        if (ExternalWorkflowExecutionSignaled == type()) { return null; }
        if (RequestCancelExternalWorkflowExecutionInitiated == type()) { return null; }
        if (RequestCancelExternalWorkflowExecutionFailed == type()) { return null; }
        if (ExternalWorkflowExecutionCancelRequested == type()) { return null; }
        throw new IllegalArgumentException("Unknown EventType " + type());
    }

    public String control() {
        if (WorkflowExecutionStarted == type()) { return null; }
        if (WorkflowExecutionCancelRequested == type()) { return null; }
        if (WorkflowExecutionCompleted == type()) { return null; }
        if (CompleteWorkflowExecutionFailed == type()) { return null; }
        if (WorkflowExecutionFailed == type()) { return null; }
        if (FailWorkflowExecutionFailed == type()) { return null; }
        if (WorkflowExecutionTimedOut == type()) { return null; }
        if (WorkflowExecutionCanceled == type()) { return null; }
        if (CancelWorkflowExecutionFailed == type()) { return null; }
        if (WorkflowExecutionContinuedAsNew == type()) { return null; }
        if (ContinueAsNewWorkflowExecutionFailed == type()) { return null; }
        if (WorkflowExecutionTerminated == type()) { return null; }
        if (DecisionTaskScheduled == type()) { return null; }
        if (DecisionTaskStarted == type()) { return null; }
        if (DecisionTaskCompleted == type()) { return null; }
        if (DecisionTaskTimedOut == type()) { return null; }
        if (ActivityTaskScheduled == type()) { return historyEvent().getActivityTaskScheduledEventAttributes().getControl(); }
        if (ScheduleActivityTaskFailed == type()) { return null; }
        if (ActivityTaskStarted == type()) { return null; }
        if (ActivityTaskCompleted == type()) { return null; }
        if (ActivityTaskFailed == type()) { return null; }
        if (ActivityTaskTimedOut == type()) { return null; }
        if (ActivityTaskCanceled == type()) { return null; }
        if (ActivityTaskCancelRequested == type()) { return null; }
        if (RequestCancelActivityTaskFailed == type()) { return null; }
        if (WorkflowExecutionSignaled == type()) { return null; }
        if (MarkerRecorded == type()) { return null; }
        if (RecordMarkerFailed == type()) { return null; }
        if (TimerStarted == type()) { return historyEvent().getTimerStartedEventAttributes().getControl(); }
        if (StartTimerFailed == type()) { return null; }
        if (TimerFired == type()) { return null; }
        if (TimerCanceled == type()) { return null; }
        if (CancelTimerFailed == type()) { return null; }
        if (StartChildWorkflowExecutionInitiated == type()) { return historyEvent().getStartChildWorkflowExecutionInitiatedEventAttributes().getControl(); }
        if (StartChildWorkflowExecutionFailed == type()) { return historyEvent().getStartChildWorkflowExecutionFailedEventAttributes().getControl(); }
        if (ChildWorkflowExecutionStarted == type()) { return null; }
        if (ChildWorkflowExecutionCompleted == type()) { return null; }
        if (ChildWorkflowExecutionFailed == type()) { return null; }
        if (ChildWorkflowExecutionTimedOut == type()) { return null; }
        if (ChildWorkflowExecutionCanceled == type()) { return null; }
        if (ChildWorkflowExecutionTerminated == type()) { return null; }
        if (SignalExternalWorkflowExecutionInitiated == type()) { return historyEvent().getSignalExternalWorkflowExecutionInitiatedEventAttributes().getControl(); }
        if (SignalExternalWorkflowExecutionFailed == type()) { return historyEvent().getSignalExternalWorkflowExecutionFailedEventAttributes().getControl(); }
        if (ExternalWorkflowExecutionSignaled == type()) { return null; }
        if (RequestCancelExternalWorkflowExecutionInitiated == type()) { return historyEvent().getRequestCancelExternalWorkflowExecutionInitiatedEventAttributes().getControl(); }
        if (RequestCancelExternalWorkflowExecutionFailed == type()) { return historyEvent().getRequestCancelExternalWorkflowExecutionFailedEventAttributes().getControl(); }
        if (ExternalWorkflowExecutionCancelRequested == type()) { return null; }
        throw new IllegalArgumentException("Unknown EventType " + type());
    }

    public String output() {
        if (WorkflowExecutionStarted == type()) { return null; }
        if (WorkflowExecutionCancelRequested == type()) { return null; }
        if (WorkflowExecutionCompleted == type()) { return historyEvent().getWorkflowExecutionCompletedEventAttributes().getResult(); }
        if (CompleteWorkflowExecutionFailed == type()) { return null; }
        if (WorkflowExecutionFailed == type()) { return null; }
        if (FailWorkflowExecutionFailed == type()) { return null; }
        if (WorkflowExecutionTimedOut == type()) { return null; }
        if (WorkflowExecutionCanceled == type()) { return null; }
        if (CancelWorkflowExecutionFailed == type()) { return null; }
        if (WorkflowExecutionContinuedAsNew == type()) { return null; }
        if (ContinueAsNewWorkflowExecutionFailed == type()) { return null; }
        if (WorkflowExecutionTerminated == type()) { return null; }
        if (DecisionTaskScheduled == type()) { return null; }
        if (DecisionTaskStarted == type()) { return null; }
        if (DecisionTaskCompleted == type()) { return historyEvent().getDecisionTaskCompletedEventAttributes().getExecutionContext(); }
        if (DecisionTaskTimedOut == type()) { return null; }
        if (ActivityTaskScheduled == type()) { return null; }
        if (ScheduleActivityTaskFailed == type()) { return null; }
        if (ActivityTaskStarted == type()) { return null; }
        if (ActivityTaskCompleted == type()) { return historyEvent().getActivityTaskCompletedEventAttributes().getResult(); }
        if (ActivityTaskFailed == type()) { return null; }
        if (ActivityTaskTimedOut == type()) { return null; }
        if (ActivityTaskCanceled == type()) { return null; }
        if (ActivityTaskCancelRequested == type()) { return null; }
        if (RequestCancelActivityTaskFailed == type()) { return null; }
        if (WorkflowExecutionSignaled == type()) { return historyEvent().getWorkflowExecutionSignaledEventAttributes().getInput(); }
        if (MarkerRecorded == type()) { return historyEvent().getMarkerRecordedEventAttributes().getDetails(); }
        if (RecordMarkerFailed == type()) { return null; }
        if (TimerStarted == type()) { return null; }
        if (StartTimerFailed == type()) { return null; }
        if (TimerFired == type()) { return "Timer Fired"; }
        if (TimerCanceled == type()) { return "Timer Canceled"; }
        if (CancelTimerFailed == type()) { return null; }
        if (StartChildWorkflowExecutionInitiated == type()) { return null; }
        if (StartChildWorkflowExecutionFailed == type()) { return null; }
        if (ChildWorkflowExecutionStarted == type()) { return null; }
        if (ChildWorkflowExecutionCompleted == type()) { return historyEvent().getChildWorkflowExecutionCompletedEventAttributes().getResult(); }
        if (ChildWorkflowExecutionFailed == type()) { return null; }
        if (ChildWorkflowExecutionTimedOut == type()) { return null; }
        if (ChildWorkflowExecutionCanceled == type()) { return null; }
        if (ChildWorkflowExecutionTerminated == type()) { return null; }
        if (SignalExternalWorkflowExecutionInitiated == type()) { return null; }
        if (SignalExternalWorkflowExecutionFailed == type()) { return null; }
        if (ExternalWorkflowExecutionSignaled == type()) { return historyEvent().getExternalWorkflowExecutionSignaledEventAttributes().getWorkflowExecution().getRunId(); }
        if (RequestCancelExternalWorkflowExecutionInitiated == type()) { return null; }
        if (RequestCancelExternalWorkflowExecutionFailed == type()) { return null; }
        if (ExternalWorkflowExecutionCancelRequested == type()) { return null; }
        throw new IllegalArgumentException("Unknown EventType " + type());
    }

    public String reason() {
        if (WorkflowExecutionStarted == type()) { return null; }
        if (WorkflowExecutionCancelRequested == type()) { return "Workflow Execution Cancel Requested"; }
        if (WorkflowExecutionCompleted == type()) { return null; }
        if (CompleteWorkflowExecutionFailed == type()) { return "Complete Workflow Execution Failed"; }
        if (WorkflowExecutionFailed == type()) { return "Workflow Execution Failed"; }
        if (FailWorkflowExecutionFailed == type()) { return "Fail Workflow Execution Failed"; }
        if (WorkflowExecutionTimedOut == type()) { return "Workflow Execution Timed Out"; }
        if (WorkflowExecutionCanceled == type()) { return "Workflow Execution Canceled"; }
        if (CancelWorkflowExecutionFailed == type()) { return "Cancel Workflow Execution Failed"; }
        if (WorkflowExecutionContinuedAsNew == type()) { return null; }
        if (ContinueAsNewWorkflowExecutionFailed == type()) { return "Continue As New Workflow Execution Failed"; }
        if (WorkflowExecutionTerminated == type()) { return "Workflow Execution Terminated"; }
        if (DecisionTaskScheduled == type()) { return null; }
        if (DecisionTaskStarted == type()) { return null; }
        if (DecisionTaskCompleted == type()) { return null; }
        if (DecisionTaskTimedOut == type()) { return null; }
        if (ActivityTaskScheduled == type()) { return null; }
        if (ScheduleActivityTaskFailed == type()) { return "Schedule Activity Task Failed"; }
        if (ActivityTaskStarted == type()) { return null; }
        if (ActivityTaskCompleted == type()) { return null; }
        if (ActivityTaskFailed == type()) { return historyEvent().getActivityTaskFailedEventAttributes().getReason(); }
        if (ActivityTaskTimedOut == type()) { return historyEvent().getActivityTaskTimedOutEventAttributes().getTimeoutType(); }
        if (ActivityTaskCanceled == type()) { return "Activity Task Canceled"; }
        if (ActivityTaskCancelRequested == type()) { return "Activity Task Cancel Requested"; }
        if (RequestCancelActivityTaskFailed == type()) { return "Request Cancel Activity Task Failed"; }
        if (WorkflowExecutionSignaled == type()) { return null; }
        if (MarkerRecorded == type()) { return null; }
        if (RecordMarkerFailed == type()) { return "Record Marker Failed"; }
        if (TimerStarted == type()) { return null; }
        if (StartTimerFailed == type()) { return "Start Timer Failed"; }
        if (TimerFired == type()) { return null; }
        if (TimerCanceled == type()) { return null; }
        if (CancelTimerFailed == type()) { return null; }
        if (StartChildWorkflowExecutionInitiated == type()) { return null; }
        if (StartChildWorkflowExecutionFailed == type()) { return "Start Child Workflow Execution Failed"; }
        if (ChildWorkflowExecutionStarted == type()) { return null; }
        if (ChildWorkflowExecutionCompleted == type()) { return null; }
        if (ChildWorkflowExecutionFailed == type()) { return historyEvent().getChildWorkflowExecutionFailedEventAttributes().getReason(); }
        if (ChildWorkflowExecutionTimedOut == type()) { return "Child Workflow Execution Timed Out"; }
        if (ChildWorkflowExecutionCanceled == type()) { return "Child Workflow Execution Canceled"; }
        if (ChildWorkflowExecutionTerminated == type()) { return "Child Workflow Execution Terminated"; }
        if (SignalExternalWorkflowExecutionInitiated == type()) { return null; }
        if (SignalExternalWorkflowExecutionFailed == type()) { return "Signal External Workflow Execution Failed"; }
        if (ExternalWorkflowExecutionSignaled == type()) { return null; }
        if (RequestCancelExternalWorkflowExecutionInitiated == type()) { return null; }
        if (RequestCancelExternalWorkflowExecutionFailed == type()) { return "Request Cancel External Workflow Execution Failed"; }
        if (ExternalWorkflowExecutionCancelRequested == type()) { return null; }
        throw new IllegalArgumentException("Unknown EventType " + type());
    }

    public String details() {
        if (WorkflowExecutionStarted == type()) { return null; }
        if (WorkflowExecutionCancelRequested == type()) { return historyEvent().getWorkflowExecutionCancelRequestedEventAttributes().getCause(); }
        if (WorkflowExecutionCompleted == type()) { return null; }
        if (CompleteWorkflowExecutionFailed == type()) { return historyEvent().getCompleteWorkflowExecutionFailedEventAttributes().getCause(); }
        if (WorkflowExecutionFailed == type()) { return historyEvent().getWorkflowExecutionFailedEventAttributes().getDetails(); }
        if (FailWorkflowExecutionFailed == type()) { return historyEvent().getFailWorkflowExecutionFailedEventAttributes().getCause(); }
        if (WorkflowExecutionTimedOut == type()) { return null; }
        if (WorkflowExecutionCanceled == type()) { return historyEvent().getWorkflowExecutionCanceledEventAttributes().getDetails(); }
        if (CancelWorkflowExecutionFailed == type()) { return historyEvent().getCancelWorkflowExecutionFailedEventAttributes().getCause(); }
        if (WorkflowExecutionContinuedAsNew == type()) { return null; }
        if (ContinueAsNewWorkflowExecutionFailed == type()) { return historyEvent().getContinueAsNewWorkflowExecutionFailedEventAttributes().getCause(); }
        if (WorkflowExecutionTerminated == type()) { return historyEvent().getWorkflowExecutionTerminatedEventAttributes().getDetails(); }
        if (DecisionTaskScheduled == type()) { return null; }
        if (DecisionTaskStarted == type()) { return null; }
        if (DecisionTaskCompleted == type()) { return null; }
        if (DecisionTaskTimedOut == type()) { return null; }
        if (ActivityTaskScheduled == type()) { return null; }
        if (ScheduleActivityTaskFailed == type()) { return historyEvent().getScheduleActivityTaskFailedEventAttributes().getCause(); }
        if (ActivityTaskStarted == type()) { return null; }
        if (ActivityTaskCompleted == type()) { return null; }
        if (ActivityTaskFailed == type()) { return historyEvent().getActivityTaskFailedEventAttributes().getDetails(); }
        if (ActivityTaskTimedOut == type()) { return historyEvent().getActivityTaskTimedOutEventAttributes().getDetails(); }
        if (ActivityTaskCanceled == type()) { return historyEvent().getActivityTaskCanceledEventAttributes().getDetails(); }
        if (ActivityTaskCancelRequested == type()) { return null; }
        if (RequestCancelActivityTaskFailed == type()) { return historyEvent().getRequestCancelActivityTaskFailedEventAttributes().getCause(); }
        if (WorkflowExecutionSignaled == type()) { return null; }
        if (MarkerRecorded == type()) { return historyEvent().getMarkerRecordedEventAttributes().getDetails(); }
        if (RecordMarkerFailed == type()) { return historyEvent().getRecordMarkerFailedEventAttributes().getCause(); }
        if (TimerStarted == type()) { return null; }
        if (StartTimerFailed == type()) { return historyEvent().getStartTimerFailedEventAttributes().getCause(); }
        if (TimerFired == type()) { return null; }
        if (TimerCanceled == type()) { return null; }
        if (CancelTimerFailed == type()) { return null; }
        if (StartChildWorkflowExecutionInitiated == type()) { return null; }
        if (StartChildWorkflowExecutionFailed == type()) { return historyEvent().getStartChildWorkflowExecutionFailedEventAttributes().getCause(); }
        if (ChildWorkflowExecutionStarted == type()) { return null; }
        if (ChildWorkflowExecutionCompleted == type()) { return null; }
        if (ChildWorkflowExecutionFailed == type()) { return historyEvent().getChildWorkflowExecutionFailedEventAttributes().getDetails(); }
        if (ChildWorkflowExecutionTimedOut == type()) { return historyEvent().getChildWorkflowExecutionTimedOutEventAttributes().getTimeoutType(); }
        if (ChildWorkflowExecutionCanceled == type()) { return historyEvent().getChildWorkflowExecutionCanceledEventAttributes().getDetails(); }
        if (ChildWorkflowExecutionTerminated == type()) { return historyEvent().getChildWorkflowExecutionTerminatedEventAttributes().getWorkflowExecution().getRunId(); }
        if (SignalExternalWorkflowExecutionInitiated == type()) { return null; }
        if (SignalExternalWorkflowExecutionFailed == type()) { return historyEvent().getSignalExternalWorkflowExecutionFailedEventAttributes().getCause(); }
        if (ExternalWorkflowExecutionSignaled == type()) { return null; }
        if (RequestCancelExternalWorkflowExecutionInitiated == type()) { return null; }
        if (RequestCancelExternalWorkflowExecutionFailed == type()) { return historyEvent().getRequestCancelExternalWorkflowExecutionFailedEventAttributes().getCause(); }
        if (ExternalWorkflowExecutionCancelRequested == type()) { return null; }
        throw new IllegalArgumentException("Unknown EventType " + type());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(1000);
        sb.append(format("%s: %s, %s, ", type(), id(), initialEventId()));
        appendIf(actionId(), sb);
        appendIf(input(), sb);
        appendIf(output(), sb);
        appendIf(control(), sb);
        appendIf(reason(), sb);
        sb.append(" ");
        sb.append(eventTimestamp());
        return sb.toString();
    }

    private static void appendIf(Object value, StringBuilder sb) {
        if (value != null) {
            sb.append(" ");
            sb.append(value);
            sb.append(",");
        }
    }

    /**
     * Sort by eventId descending (most recent event first).
     */
    @Override
    public int compareTo(Event event) {
            return event.id().compareTo(id());
    }
}
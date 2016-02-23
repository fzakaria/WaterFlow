package com.github.fzakaria.waterflow;

/**
 * A categorized type of tasks that can be executed.
 * The list is mainly made from grouping similar decision types listed
 * <a href="http://docs.aws.amazon.com/amazonswf/latest/apireference/API_Decision.html">here.</a>
 */
public enum TaskType {
    ACTIVITY,
    TIMER,
    START_CHILD_WORKFLOW,
    RECORD_MARKER,
    SIGNAL_EXTERNAL_WORKFLOW,
    CANCEL_EXTERNAL_WORKFLOW,
    CONTINUE_AS_NEW,
    DECISION,
    WORKFLOW_EXECUTION,
    WORKFLOW_SIGNALED
}
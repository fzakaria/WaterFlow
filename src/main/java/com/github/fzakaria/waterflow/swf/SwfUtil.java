package com.github.fzakaria.waterflow.swf;


import com.amazonaws.services.simpleworkflow.model.CancelTimerDecisionAttributes;
import com.amazonaws.services.simpleworkflow.model.CancelWorkflowExecutionDecisionAttributes;
import com.amazonaws.services.simpleworkflow.model.CompleteWorkflowExecutionDecisionAttributes;
import com.amazonaws.services.simpleworkflow.model.ContinueAsNewWorkflowExecutionDecisionAttributes;
import com.amazonaws.services.simpleworkflow.model.Decision;
import com.amazonaws.services.simpleworkflow.model.DecisionType;
import com.amazonaws.services.simpleworkflow.model.FailWorkflowExecutionDecisionAttributes;
import com.amazonaws.services.simpleworkflow.model.RecordMarkerDecisionAttributes;
import com.amazonaws.services.simpleworkflow.model.RequestCancelActivityTaskDecisionAttributes;
import com.amazonaws.services.simpleworkflow.model.RequestCancelExternalWorkflowExecutionDecisionAttributes;
import com.amazonaws.services.simpleworkflow.model.ScheduleActivityTaskDecisionAttributes;
import com.amazonaws.services.simpleworkflow.model.SignalExternalWorkflowExecutionDecisionAttributes;
import com.amazonaws.services.simpleworkflow.model.StartChildWorkflowExecutionDecisionAttributes;
import com.amazonaws.services.simpleworkflow.model.StartTimerDecisionAttributes;

import static java.lang.String.format;
import static java.lang.String.valueOf;

/**
 * A utility class with some helper methods when working with SimpleWorkflow
 */
public final class SwfUtil {

    public static String assertMaxLength(String input, int max) {
        if (input.length() > max) {
            throw new IllegalStateException(format("%s has a longer allowed size than %s", input, max));
        }
        return input;
    }

    /**
     * @return true if the parameter is not null or has a length greater than zero
     */
    public static boolean isNotEmpty(String s) {
        return !(s == null || s.length() == 0);
    }

    /**
     * @return replacement parameter converted to a string if the value string is null or empty, otherwise return value string.
     */
    public static <T> String defaultIfEmpty(String value, T replacement) {
        if (isNotEmpty(value)) {
            return value;
        } else {
            return replacement == null ? null : valueOf(replacement);
        }
    }

    /**
     * Trim a string if it exceeds a maximum length.
     *
     * @param s string to trim, null allowed
     * @param maxLength max length
     *
     * @return trimmed string if it exceeded maximum length, otherwise string parameter
     */
    public static String trimToMaxLength(String s, int maxLength) {
        try {
            if (s != null && s.length() > maxLength) {
                return s.substring(0, maxLength);
            } else {
                return s;
            }
        } catch (StringIndexOutOfBoundsException e) {
            throw new IllegalArgumentException(format("trimToMaxLength(%s, %d)", s, maxLength));
        }
    }

    /**
     * Assert the value passes the constraints for SWF fields like name, version, domain, taskList, identifiers.
     *
     * @param value to assert
     *
     * @return the parameter for method chaining
     */
    public static String assertValidSwfValue(String value) {
        if (value != null) {
            if (value.length() == 0) {
                throw new AssertionError("Empty value not allowed");
            }
            if (value.matches("\\s.*|.*\\s")
                    || value.matches(".*[:/|\\u0000-\\u001f\\u007f-\\u009f].*")
                    || value.contains("arn")) {
                throw new AssertionError("Value contains one or more bad characters: '" + value + "'");
            }
        }
        return value;
    }

    /**
     * Create a nice log message based on the {@link DecisionType} for the given decision.
     */
    public static String logNiceDecision(Decision decision) {
        String decisionType = decision.getDecisionType();
        switch (DecisionType.valueOf(decision.getDecisionType())) {
            case ScheduleActivityTask:
                ScheduleActivityTaskDecisionAttributes a1 = decision.getScheduleActivityTaskDecisionAttributes();
                return format("%s['%s' '%s': %s %s]", decisionType, a1.getActivityId(), a1.getActivityType().getName(), a1.getInput(), a1.getControl());
            case CompleteWorkflowExecution:
                CompleteWorkflowExecutionDecisionAttributes a2 = decision.getCompleteWorkflowExecutionDecisionAttributes();
                return format("%s[%s]", decisionType, a2.getResult());
            case FailWorkflowExecution:
                FailWorkflowExecutionDecisionAttributes a3 = decision.getFailWorkflowExecutionDecisionAttributes();
                return format("%s[%s %s]", decisionType, a3.getReason(), a3.getDetails());
            case CancelWorkflowExecution:
                CancelWorkflowExecutionDecisionAttributes a4 = decision.getCancelWorkflowExecutionDecisionAttributes();
                return format("%s[%s]", decisionType, a4.getDetails());
            case ContinueAsNewWorkflowExecution:
                ContinueAsNewWorkflowExecutionDecisionAttributes a5 = decision.getContinueAsNewWorkflowExecutionDecisionAttributes();
                return format("%s[%s]", decisionType, a5.getInput());
            case RecordMarker:
                RecordMarkerDecisionAttributes a6 = decision.getRecordMarkerDecisionAttributes();
                return format("%s['%s': %s]", decisionType, a6.getMarkerName(), a6.getDetails());
            case StartTimer:
                StartTimerDecisionAttributes a7 = decision.getStartTimerDecisionAttributes();
                return format("%s['%s': %s]", decisionType, a7.getTimerId(), a7.getControl());
            case CancelTimer:
                CancelTimerDecisionAttributes a8 = decision.getCancelTimerDecisionAttributes();
                return format("%s['%s']", decisionType, a8.getTimerId());
            case SignalExternalWorkflowExecution:
                SignalExternalWorkflowExecutionDecisionAttributes a9 = decision.getSignalExternalWorkflowExecutionDecisionAttributes();
                return format("%s['%s' wf='%s' runId='%s': '%s' '%s']", decisionType, a9.getSignalName(), a9.getWorkflowId(), a9.getRunId(), a9.getInput(), a9.getControl());
            case RequestCancelExternalWorkflowExecution:
                RequestCancelExternalWorkflowExecutionDecisionAttributes a10 = decision.getRequestCancelExternalWorkflowExecutionDecisionAttributes();
                return format("%s[wf='%s' runId='%s': '%s']", decisionType, a10.getWorkflowId(), a10.getRunId(), a10.getControl());
            case StartChildWorkflowExecution:
                StartChildWorkflowExecutionDecisionAttributes a11 = decision.getStartChildWorkflowExecutionDecisionAttributes();
                return format("%s['%s' '%s': '%s' '%s']", decisionType, a11.getWorkflowId(), a11.getWorkflowType().getName(), a11.getInput(), a11.getControl());
            case RequestCancelActivityTask:
                RequestCancelActivityTaskDecisionAttributes a12 = decision.getRequestCancelActivityTaskDecisionAttributes();
                return format("%s[%s]", decisionType, a12.getActivityId());
        }
        return null;
    }




}

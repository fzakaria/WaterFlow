package com.github.fzakaria.waterflow;

import lombok.experimental.UtilityClass;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static com.github.fzakaria.waterflow.SwfConstants.*;
import static java.lang.String.format;
import static java.lang.String.valueOf;

/**
 * A utility class with some helper methods when working with SimpleWorkflow
 */
@UtilityClass
public class SwfUtil {

    /**
     * Calc a SWF timeout string.
     * Pass null unit or duration &lt;= 0 for a timeout of NONE.
     *
     * @param unit time unit to use with duration
     * @param duration duration converted to seconds
     *
     * @see #calcTimeoutOrYear(TimeUnit, long)
     */
    public static String calcTimeoutOrNone(TimeUnit unit, long duration) {
        return unit == null || duration < 1 ? SWF_TIMEOUT_NONE : valueOf(unit.toSeconds(duration));
    }

    /**
     * Calc a SWF timeout string.
     * Pass null unit or duration &lt;= 0 for a timeout of 365 days.
     * <p/>
     * Some SWF timeouts, specifically workflow execution start to close timeouts cannot be set to "NONE".
     * Instead a maximum duration of 365 days is used for the default.
     *
     * @param unit time unit to use with duration
     * @param duration duration converted to seconds
     *
     * @see #calcTimeoutOrNone(TimeUnit, long)
     */
    public static String calcTimeoutOrYear(TimeUnit unit, long duration) {
        return unit == null || duration < 1 ? SWF_TIMEOUT_YEAR : valueOf(unit.toSeconds(duration));
    }


    /**
     * Combine a name and version into a single string for easier indexing in maps, etc.
     * In SWF registered workflows and activities are identified by the combination of name and version.
     */
    public static String makeKey(String name, String version) {
        return format("%s-%s", name, version);
    }

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
            if (value.length() == 0
                    || value.matches("\\s.*|.*\\s")
                    || value.matches(".*[:/|\\u0000-\\u001f\\u007f-\\u009f].*")
                    || value.contains("arn")) {
                throw new AssertionError("Value contains one or more bad characters: '" + value + "'");
            }
        }
        return value;
    }

    /**
     * Make a unique and valid workflowId.
     * Replaces bad characters and whitespace, appends a random int, and trims to {@link MAX_ID_LENGTH}, which also makes it easy for amazon cli use.
     *
     * @param workflowName name of workflow.
     *
     * @return unique workflowId
     */
    public static String createUniqueWorkflowId(String workflowName) {
        String name = replaceUnsafeNameChars(workflowName);
        String randomize = format(".%010d", ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE));
        name = trimToMaxLength(name, MAX_ID_LENGTH - randomize.length());
        return assertValidSwfValue(name + randomize);
    }

    /**
     * Replace disallowed name characters and whitespace with an underscore.
     *
     * @param string string to be fixed
     *
     * @return string with replacements
     */
    public static String replaceUnsafeNameChars(String string) {
        return string.trim()
                .replaceAll("\\s|[^\\w]", "_")
                .replaceAll("arn", "Arn");
    }

}

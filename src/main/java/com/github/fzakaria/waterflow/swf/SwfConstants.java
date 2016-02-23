package com.github.fzakaria.waterflow.swf;

import com.github.fzakaria.waterflow.immutable.Description;
import com.github.fzakaria.waterflow.immutable.TaskListName;

import java.time.Duration;
import java.time.Period;

/**
 * This is a utility class to hold all the SWF constants
 */
public final class SwfConstants {

    public final static Description DEFAULT_DESCRIPTION = Description.of("WaterFlow Workflow");

    public final static TaskListName DEFAULT_TASK_LIST = TaskListName.of("DEFAULT");

    public final static Period MAX_DOMAIN_RETENTION = Period.ofDays(90);

    public static final int MAX_REASON_LENGTH = 256;

    public static final int MAX_RESULT_LENGTH = 32768;

    public static final String SWF_TIMEOUT_NONE = "NONE";

    public static final Duration SWF_TIMEOUT_YEAR = Duration.ofDays(356);

    public static final Duration SWF_TIMEOUT_DECISION_DEFAULT = Duration.ofMinutes(1);

    public static final int MAX_NAME_LENGTH = 256;

    public static final int MAX_VERSION_LENGTH = 64;

    public static final int MAX_RUN_ID_LENGTH = 64;

    public static final int MAX_NUMBER_TAGS = 5;

    public static final int MAX_ID_LENGTH = 256;

    public static final int MAX_DESCRIPTION_LENGTH = 1024;

    public static final int MAX_DETAILS_LENGTH = 32768;

    public static final int MAX_CONTROL_LENGTH = 32768;

    public static final int MAX_INPUT_LENGTH = 32768;

    public static final int MARKER_NAME_MAX_LENGTH = 256;




}

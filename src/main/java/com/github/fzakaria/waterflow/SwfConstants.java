package com.github.fzakaria.waterflow;

import lombok.experimental.UtilityClass;

import java.time.Period;
import java.util.concurrent.TimeUnit;

/**
 * This is a utility class to hold all the SWF constants
 */
@UtilityClass
public class SwfConstants {

    //The maximum workflow execution retention period is 90 days
    public final static Period MAX_DOMAIN_RETENTION = Period.ofDays(90);

    public static final int MAX_REASON_LENGTH = 256;

    public static final int MAX_RESULT_LENGTH = 32768;

    public static final String SWF_TIMEOUT_NONE = "NONE";

    public static final String SWF_TIMEOUT_YEAR = String.valueOf((TimeUnit.DAYS.toSeconds(365)));

    public static final String SWF_TIMEOUT_DECISION_DEFAULT = String.valueOf(TimeUnit.MINUTES.toSeconds(1));

    public static final int MAX_NAME_LENGTH = 256;

    public static final int MAX_VERSION_LENGTH = 64;

    public static final int MAX_RUN_ID_LENGTH = 64;

    public static final int MAX_NUMBER_TAGS = 5;

    public static final int MAX_ID_LENGTH = 256;

    public static final int MAX_DESCRIPTION_LENGTH = 1024;

    public static final int MAX_DETAILS_LENGTH = 32768;

    public static final int MARKER_NAME_MAX_LENGTH = 256;




}

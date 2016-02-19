package com.github.fzakaria.waterflow;

import com.amazonaws.services.simpleworkflow.model.RecordActivityTaskHeartbeatRequest;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

import static com.github.fzakaria.waterflow.SwfConstants.MAX_DETAILS_LENGTH;
import static com.github.fzakaria.waterflow.SwfUtil.assertMaxLength;


/**
 * The abstract class of all classes that hold Swf Activity implementations.
 * This gives consistent access to some helper members such as {@link #activityContext}
 */
public abstract class Activities {

    private static final Logger log = LoggerFactory.getLogger(Activities.class);

    @Nullable
    public abstract ActivityContext activityContext();

    /**
     * Record a heartbeat on SWF.
     * @param details information to be recorded
     */
    protected void recordHeartbeat(String details) {
        final String taskToken = activityContext().task().getTaskToken();
        try {
            activityContext().service().recordActivityTaskHeartbeat(createRecordActivityTaskHeartbeat(taskToken, details));
        } catch (Throwable e) {
            log.warn("Failed to record heartbeat: " + taskToken + ", " + details, e);
        }
    }

    public static RecordActivityTaskHeartbeatRequest createRecordActivityTaskHeartbeat(String taskToken, String details) {
        return new RecordActivityTaskHeartbeatRequest()
                .withTaskToken(taskToken)
                .withDetails(assertMaxLength(details, MAX_DETAILS_LENGTH));
    }
}

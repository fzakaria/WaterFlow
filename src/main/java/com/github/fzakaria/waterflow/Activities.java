package com.github.fzakaria.waterflow;

import com.amazonaws.services.simpleworkflow.model.RecordActivityTaskHeartbeatRequest;
import com.github.fzakaria.waterflow.immutable.Details;
import com.github.fzakaria.waterflow.swf.RecordActivityTaskHeartbeatRequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

import static com.github.fzakaria.waterflow.swf.SwfConstants.MAX_DETAILS_LENGTH;
import static com.github.fzakaria.waterflow.swf.SwfUtil.assertMaxLength;


/**
 * The abstract class of all classes that hold Swf Activity implementations.
 * This gives consistent access to some helper members such as {@link #activityContext}
 */
public abstract class Activities {

    private static final Logger log = LoggerFactory.getLogger(Activities.class);

    private ActivityContext activityContext;

    public ActivityContext activityContext() {
        return activityContext;
    }

    /**
     * Record a heartbeat on SWF.
     * @param details information to be recorded
     */
    protected void recordHeartbeat(String details) {
        final String taskToken = activityContext().task().getTaskToken();
        try {
            final RecordActivityTaskHeartbeatRequest request =
                    RecordActivityTaskHeartbeatRequestBuilder.builder()
                            .taskToken(taskToken).details(Details.of(details)).build();
            activityContext().service().recordActivityTaskHeartbeat(request);
        } catch (Throwable e) {
            log.warn("Failed to record heartbeat: " + taskToken + ", " + details, e);
            throw e;
        }
    }

}

package com.github.fzakaria.waterflow;

import com.amazonaws.services.simpleworkflow.model.RecordActivityTaskHeartbeatRequest;
import com.github.fzakaria.waterflow.immutable.Details;
import com.github.fzakaria.waterflow.swf.RecordActivityTaskHeartbeatRequestBuilder;
import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;


/**
 * The abstract class of all classes that hold Swf Activity implementations.
 * This gives consistent access to some helper members such as {@link #activityContext}
 */
public abstract class Activities {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Nullable
    private ActivityContext activityContext;

    public ActivityContext activityContext() {
        return activityContext;
    }

    public Activities activityContext(ActivityContext activityContext) {
        this.activityContext = activityContext;
        return this;
    }

    /**
     * Record a heartbeat on SWF.
     * @param details information to be recorded
     */
    protected void recordHeartbeat(String details) {
        Preconditions.checkNotNull(activityContext, "ActivityContext should have been set.");
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

package com.github.fzakaria.waterflow.retry;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.time.Instant;

/**
 *  An interface for figuring out the duration for the next
 *  {@link com.github.fzakaria.waterflow.action.ActivityAction}
 */
public interface RetryStrategy {

    @Nonnull Duration nextRetry(long attempt, @Nonnull Instant startTime);
}
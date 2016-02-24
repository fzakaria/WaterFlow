package com.github.fzakaria.waterflow.retry;

import java.time.Duration;
import java.time.Instant;

/**
 * Creates a {@link RetryStrategy} that will increase delay exponentially between each retries.
 */
public class ExponentialDelayRetryStrategy implements RetryStrategy {

    private final Duration baseDelay;

    public ExponentialDelayRetryStrategy(Duration baseDelay) {
        this.baseDelay = baseDelay;
    }

    @Override
    public Duration nextRetry(long attempt, Instant startTime) {
        long power = (1 << attempt) - 1;
        return baseDelay.multipliedBy(power);
    }
}
package com.github.fzakaria.waterflow.retry;


import java.time.Duration;
import java.time.Instant;

/**
 * Creates a {@link RetryStrategy} that imposes a fix delay between each retries.
 */
public class FixedDelayRetryStrategy implements RetryStrategy {

    private final Duration delay;

    public FixedDelayRetryStrategy(Duration delay) {
        this.delay = delay;
    }

    @Override
    public Duration nextRetry(long attempt, Instant startTime) {
        return delay;
    }

}
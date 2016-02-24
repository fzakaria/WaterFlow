package com.github.fzakaria.waterflow.retry;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Creates a sensible default {@link RetryStrategy}.
 * The retry strategy has a maximum time limit of 2 hours.
 * The retry strategy has a maximum attempt limit of 100.
 * The retry strategy has a initial delay of 1 minute.
 * The retry strategy grows exponentially.
 */
public class DefaultRetryStrategy implements RetryStrategy {

    private final RetryStrategy retryStrategy;

    private static final Duration MAX_TIME_LIMIT = Duration.ofHours(2);

    private static final Duration INITIAL_DELAY = Duration.ofMinutes(1);

    private static final long MAX_ATTEMPTS = 100;

    public DefaultRetryStrategy(Clock clock) {
        RetryStrategy backOffStratgy = new ExponentialDelayRetryStrategy(INITIAL_DELAY);
        RetryStrategy maxLimitStrategy = new MaxLimitRetryStrategy(backOffStratgy, MAX_ATTEMPTS);
        retryStrategy = new TimeLimitRetryStrategy(maxLimitStrategy, clock, MAX_TIME_LIMIT);
    }


    @Override
    public Duration nextRetry(long attempt, Instant startTime) {
        return retryStrategy.nextRetry(attempt, startTime);
    }
}
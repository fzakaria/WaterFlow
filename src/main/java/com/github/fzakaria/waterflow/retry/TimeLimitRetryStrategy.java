package com.github.fzakaria.waterflow.retry;

import com.github.fzakaria.waterflow.retry.RetryStrategy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Creates a {@link RetryStrategy} that will retry until maximum amount of time has been passed since the request,
 * with the actual delay behavior delegated to another {@link RetryStrategy}.
 */
public class TimeLimitRetryStrategy implements RetryStrategy {

    private final Clock clock;

    private final RetryStrategy delegate;

    private final Duration maxElapsedTime;

    public TimeLimitRetryStrategy(RetryStrategy delegate, Clock clock, Duration maxElapsedTime) {
        this.clock = clock;
        this.delegate = delegate;
        this.maxElapsedTime = maxElapsedTime;
    }

    @Override
    public Duration nextRetry(long attempt, Instant startTime) {
        Instant maxElapsedInstant = startTime.plus(maxElapsedTime);
        Instant currentElapsedDate = clock.instant();
        return currentElapsedDate.isBefore(maxElapsedInstant) ? delegate.nextRetry(attempt, startTime) : Duration.ZERO;
    }
}
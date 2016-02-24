package com.github.fzakaria.waterflow.retry;


import com.google.common.base.Preconditions;

import java.time.Duration;
import java.time.Instant;

/**
 * Creates a {@link RetryStrategy} that retries maximum given number of times, with the actual
 * delay behavior delegated to another {@link RetryStrategy}.
 */
public class MaxLimitRetryStrategy implements RetryStrategy{

    private final RetryStrategy delegate;

    private final long limit;

    public MaxLimitRetryStrategy(RetryStrategy delegate, long limit) {
        Preconditions.checkArgument(limit >= 0, "Limit must be greater than 0");
        this.delegate = delegate;
        this.limit = limit;
    }

    @Override
    public Duration nextRetry(long attempt, Instant startTime) {
        return (attempt <= limit) ? delegate.nextRetry(attempt, startTime) : Duration.ZERO;
    }
}
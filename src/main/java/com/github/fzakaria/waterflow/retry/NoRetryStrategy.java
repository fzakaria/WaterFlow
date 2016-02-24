package com.github.fzakaria.waterflow.retry;

import java.time.Duration;
import java.time.Instant;

public class NoRetryStrategy implements RetryStrategy {

    public static final RetryStrategy INSTANCE = new NoRetryStrategy();

    @Override
    public Duration nextRetry(long attempt, Instant startTime) {
        return Duration.ZERO;
    }

}
package com.github.fzakaria.waterflow;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.model.ActivityTask;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

/**
 * Access to the inner implementation details of the Activity.
 * Also includes helpful functions that ne
 */
@Value
@Accessors(fluent = true)
@Builder
@Slf4j
public class ActivityContext {

    private final AmazonSimpleWorkflow service;

    private final ActivityTask task;



}

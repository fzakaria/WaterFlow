package com.github.fzakaria.waterflow;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.model.ActivityTask;
import org.immutables.value.Value;


/**
 * Access to the inner implementation details of the Activity.
 * Also includes helpful functions that ne
 */
@Value.Immutable
public abstract class ActivityContext {

    public abstract AmazonSimpleWorkflow service();

    public abstract ActivityTask task();



}

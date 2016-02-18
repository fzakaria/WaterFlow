package com.github.fzakaria.waterflow.activity;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.model.ActivityTask;
import com.github.fzakaria.waterflow.Activities;
import com.github.fzakaria.waterflow.ActivityContext;
import com.github.fzakaria.waterflow.converter.DataConverter;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;

import java.lang.reflect.Method;

import static java.lang.String.format;

/**
 * The class/interface that is responsible from figuring out what implements the
 * specific {@link ActivityTask}\
 * This implementation is designed for use with {@link ActivityMethod} and expects
 * the input of the {@link ActivityTask} to be a {@link Object[]}
 */
@Value
@Builder
@Accessors(fluent = true)
public class ActivityInvoker {

    private final AmazonSimpleWorkflow service;
    private final ActivityMethod activityMethod;
    private final Method method;
    private final Activities instance;
    private final DataConverter dataConverter;

    /**
     * Given a {@link ActivityTask} execute matching {@link ActivityMethod}
     * The input of the ActivityTask must be a Object[]
     * @return The result of the {@link ActivityMethod} serialized
     */
    public String invoke(ActivityTask task) {
        ActivityContext context = ActivityContext.builder().task(task).service(service).build();
        instance.activityContext(context);

        String name = task.getActivityType().getName();
        Object[] input = dataConverter.fromData(task.getInput(), Object[].class);
        try {
            Object result = method.invoke(instance, input);
            return dataConverter.toData(result);
        } catch (Throwable e) {
            throw new IllegalStateException(format("error: '%s' '%s' '%s'", task.getActivityId(), name, task.getInput()), e);
        }
    }
}

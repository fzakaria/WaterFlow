package com.github.fzakaria.waterflow.activity;


import com.amazonaws.services.simpleworkflow.model.RegisterActivityTypeRequest;
import com.amazonaws.services.simpleworkflow.model.RespondActivityTaskCompletedRequest;

import com.github.fzakaria.waterflow.poller.ActivityPoller;
import org.immutables.value.Value;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a method as an activity method that can handle a registered SWF Activity Task.
 * Methods annotated with <code>ActivityMethod</code> must have the following signature:
 * <pre><code>
 * &#64;ActivityMethod(name="MyActivity", version="1.0")
 * MyFakeResultType MyFakeOutputType methodName(MyFakeInputType input) {...}
 * <p/>
 * or
 * <p/>
 * &#64;ActivityMethod(name="MyActivity", version="1.0")
 * DifferentFakeResultType methodName(MyFakeInputType input1, DifferentInputType input2) {...}
 * </code></pre>
 * <p/>
 * If the return type is void or the method returns null an empty string will be recorded as the activity task result.
 * Otherwise the result will be recorded as the return value converted to a string using toString().
 * <p/>
 * Activities that throw exceptions will be recorded as an error on their related workflow.
 *
 * @see RegisterActivityTypeRequest RegisterActivityTypeRequest for details and limits of the parameters
 * @see RespondActivityTaskCompletedRequest#result
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Value.Immutable
public @interface ActivityMethod {
    /**
     * Registered activity name.
     *
     * @see RegisterActivityTypeRequest#name
     */
    String name();

    /**
     * Registered activity version.
     *
     * @see RegisterActivityTypeRequest#version
     */
    String version();

    /**
     * Description of activity.
     * Used by {@link ActivityPoller#registerSwfActivities()} when registering the activity on a domain.
     *
     * @see RegisterActivityTypeRequest#description
     */
    String description() default "";

    /**
     * Task list.
     * Used by {@link ActivityPoller#registerSwfActivities()} when registering the activity on a domain.
     *
     * @return defaults to 'default'
     */
    String taskList() default "default";

    /**
     * Schedule to close timeout, default "NONE".
     * <pre>
     * schedule ---> start ---> close
     * |_____________________________|
     * </pre>
     * Used by {@link ActivityPoller#registerSwfActivities()} when registering the activity on a domain.
     *
     * @see RegisterActivityTypeRequest#defaultTaskScheduleToCloseTimeout
     */
    String scheduleToCloseTimeout() default "NONE";

    /**
     * Schedule to close timeout, default "NONE".
     * <pre>
     * schedule ---> start ---> close
     * |_________________|
     * </pre>
     * Used by {@link ActivityPoller#registerSwfActivities()} when registering the activity on a domain.
     *
     * @see RegisterActivityTypeRequest#defaultTaskScheduleToStartTimeout
     */
    String scheduleToStartTimeout() default "NONE";

    /**
     * Schedule to close timeout, default "NONE".
     * <pre>
     * schedule ---> start ---> close
     *              |_______________|
     * </pre>
     * Used by {@link ActivityPoller#registerSwfActivities()} when registering the activity on a domain.
     *
     * @see RegisterActivityTypeRequest#defaultTaskStartToCloseTimeout
     */
    String startToCloseTimeout() default "NONE";


    /**
     * Heartbeat timeout, default "NONE".
     * Used by {@link ActivityPoller#registerSwfActivities()} when registering the activity on a domain.
     *
     * @see RegisterActivityTypeRequest#defaultTaskHeartbeatTimeout
     */
    String heartbeatTimeout() default "NONE";

    /** Task priority values can range from "-2147483648" to "2147483647", with higher numbers indicating
     * higher priority. If you don't set the task priority for an activity or workflow,
     * it will be assigned a priority of zero ("0").
     * @see RegisterActivityTypeRequest#defaultTaskPriority
     */
    int taskPriority() default 0;

}
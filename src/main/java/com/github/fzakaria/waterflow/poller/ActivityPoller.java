package com.github.fzakaria.waterflow.poller;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.model.*;

import com.github.fzakaria.waterflow.Activities;
import com.github.fzakaria.waterflow.activity.ActivityInvoker;
import com.github.fzakaria.waterflow.activity.ActivityMethod;
import com.github.fzakaria.waterflow.converter.DataConverter;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.lang.String.format;
import static com.github.fzakaria.waterflow.SwfUtil.*;
import static com.github.fzakaria.waterflow.SwfConstants.*;
/**
 * Polls for activities on a given domain and task list and executes them.
 * <p/>
 * Implements {@link Runnable} so that multiple instances of this class can be
 * scheduled to handle higher levels of activity tasks.
 * <p/>
 * Since this class is single-threaded it will be tied-up while the activity is processing so scale
 * the size of the activity polling pool appropriately if you have many long-running activities.
 *
 * This class only recognizes activities that have been annotated with {@link ActivityMethod}
 *
 * @see BasePoller
 */
@Slf4j
@Value
@EqualsAndHashCode(callSuper = true)
@Accessors(fluent = true)
public class ActivityPoller extends BasePoller {
    private final Map<String, ActivityInvoker> activityMap = new LinkedHashMap<>();

    private final DataConverter dataConverter;

    public ActivityPoller(String id, String taskList, String domain, AmazonSimpleWorkflow swf, DataConverter dataConverter) {
        super(id, taskList, domain, swf);
        this.dataConverter = dataConverter;
    }

    /**
     * Register activities added to this poller on Amazon SWF with this instance's domain and task list.
     * {@link TypeAlreadyExistsException} are ignored making this method idempotent.
     *
     * @see ActivityMethod
     */
    public void registerSwfActivities() {
        for (ActivityInvoker invoker : activityMap.values()) {
            ActivityMethod method = invoker.activityMethod();
            String key = makeKey(method.name(), method.version());
            try {
                swf.registerActivityType(createRegisterActivityType(domain, taskList, method));
                log.info(format("Register activity succeeded %s", key));
            } catch (TypeAlreadyExistsException e) {
                log.info(format("Register activity already exists %s", key));
            } catch (Throwable t) {
                String format = format("Register activity failed %s", key);
                log.error(format, t);
                throw new IllegalStateException(format, t);
            }
        }
    }

    /**
     * Add objects with one or more methods annotated with {@link ActivityMethod}
     * mirroring Activity Types registered on SWF with this poller's domain and task list.
     *
     * @param annotatedObjects objects with one or more methods annotated with {@link ActivityMethod}
     */
    public void addActivities(Activities... annotatedObjects) {
        for (Activities object : annotatedObjects) {
            for (Method method : object.getClass().getDeclaredMethods()) {
                if (method != null && method.isAnnotationPresent(ActivityMethod.class)) {
                    ActivityMethod activityMethod = method.getAnnotation(ActivityMethod.class);
                    String key = makeKey(activityMethod.name(), activityMethod.version());
                    log.info(format("add activity %s", key));
                    ActivityInvoker activityInvoker = ActivityInvoker.builder().activityMethod(activityMethod)
                            .dataConverter(dataConverter).instance(object).method(method).build();
                    activityMap.put(key,activityInvoker);
                }
            }
        }
    }

    /**
     * Each call performs a long polling or the next activity task from SWF and then calls
     * the matching registered {@link ActivityMethod} method to perform the task.
     * <p/>
     * <ul>
     * <li>Methods that succeed will cause a {@link RespondActivityTaskCompletedRequest} to be returned.</li>
     * <li>Methods that throw methods will cause a {@link RespondActivityTaskFailedRequest} to be returned.</li>
     * <li>Methods may issue zero or more {@link RecordActivityTaskHeartbeatRequest} calls while processing</li>
     * </ul>
     *
     * @see #addActivities(Activities...)
     */
    @Override
    protected void poll() {
        ActivityTask task = swf.pollForActivityTask(createPollForActivityTask(domain, taskList, getId()));

        if (task.getTaskToken() == null) {
            return;
        }

        String input = task.getInput();
        String key = makeKey(task.getActivityType().getName(), task.getActivityType().getVersion());
        try {
            log.debug("start: {}", task);
            if (activityMap.containsKey(key)) {
                String result = activityMap.get(key).invoke(task);
                log.info("'{}' '{}' '{}' -> '{}'", task.getActivityId(), key, input, result);
                swf.respondActivityTaskCompleted(createRespondActivityCompleted(task, result));
            } else {
                String reason = format("Activity '%s' not registered on poller %s", task, getId());
                log.error(reason);
                swf.respondActivityTaskFailed(
                        createRespondActivityTaskFailed(task.getTaskToken(), reason, null)
                );
            }
        } catch (Exception e) {
            log.error("'{}' '{}' '{}'", task.getActivityId(), key, input, e);
            String details = dataConverter.toData(e);
            swf.respondActivityTaskFailed(
                    createRespondActivityTaskFailed(task.getTaskToken(), e.getMessage(), details)
            );
        }
    }



    public static RegisterActivityTypeRequest createRegisterActivityType(String domain, String taskList, ActivityMethod method) {
        return new RegisterActivityTypeRequest()
                .withDomain(domain)
                .withDefaultTaskList(new TaskList().withName(taskList))
                .withName(method.name())
                .withVersion(method.version())
                .withDescription(defaultIfEmpty(method.description(), null))
                .withDefaultTaskHeartbeatTimeout(defaultIfEmpty(method.heartbeatTimeout(), SWF_TIMEOUT_NONE))
                .withDefaultTaskStartToCloseTimeout(defaultIfEmpty(method.startToCloseTimeout(), SWF_TIMEOUT_NONE))
                .withDefaultTaskScheduleToStartTimeout(defaultIfEmpty(method.scheduleToStartTimeout(), SWF_TIMEOUT_NONE))
                .withDefaultTaskScheduleToCloseTimeout(defaultIfEmpty(method.scheduleToCloseTimeout(), SWF_TIMEOUT_NONE));
    }

    public static PollForActivityTaskRequest createPollForActivityTask(String domain, String taskList, String id) {
        return new PollForActivityTaskRequest()
                .withDomain(domain)
                .withTaskList(new TaskList()
                        .withName(taskList))
                .withIdentity(id);
    }

    public static RespondActivityTaskFailedRequest createRespondActivityTaskFailed(String taskToken, String reason, String details) {
        return new RespondActivityTaskFailedRequest()
                .withTaskToken(taskToken)
                .withReason(trimToMaxLength(reason, MAX_REASON_LENGTH))
                .withDetails(trimToMaxLength(details, MAX_DETAILS_LENGTH));
    }

    public static RespondActivityTaskCompletedRequest createRespondActivityCompleted(ActivityTask task, String result) {
        return new RespondActivityTaskCompletedRequest()
                .withTaskToken(task.getTaskToken())
                .withResult(trimToMaxLength(result, MAX_RESULT_LENGTH));
    }


}
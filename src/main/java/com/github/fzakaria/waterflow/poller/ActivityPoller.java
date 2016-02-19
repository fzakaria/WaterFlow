package com.github.fzakaria.waterflow.poller;

import com.amazonaws.services.simpleworkflow.model.ActivityTask;
import com.amazonaws.services.simpleworkflow.model.PollForActivityTaskRequest;
import com.amazonaws.services.simpleworkflow.model.RecordActivityTaskHeartbeatRequest;
import com.amazonaws.services.simpleworkflow.model.RegisterActivityTypeRequest;
import com.amazonaws.services.simpleworkflow.model.RespondActivityTaskCompletedRequest;
import com.amazonaws.services.simpleworkflow.model.RespondActivityTaskFailedRequest;
import com.amazonaws.services.simpleworkflow.model.TaskList;
import com.amazonaws.services.simpleworkflow.model.TypeAlreadyExistsException;
import com.github.fzakaria.waterflow.Activities;
import com.github.fzakaria.waterflow.activity.ActivityInvoker;
import com.github.fzakaria.waterflow.activity.ActivityMethod;
import com.github.fzakaria.waterflow.activity.ImmutableActivityInvoker;
import com.github.fzakaria.waterflow.converter.DataConverter;

import com.github.fzakaria.waterflow.immutable.Domain;
import com.github.fzakaria.waterflow.immutable.Key;
import com.github.fzakaria.waterflow.immutable.Name;
import com.github.fzakaria.waterflow.immutable.TaskListName;
import com.google.common.collect.Maps;
import org.immutables.value.Value;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static com.github.fzakaria.waterflow.SwfConstants.*;
import static com.github.fzakaria.waterflow.SwfUtil.*;
import static java.lang.String.format;
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
@Value.Immutable
public abstract class ActivityPoller extends BasePoller {

    public abstract List<Activities> activities();

    public abstract DataConverter dataConverter();

    @Value.Derived
    public Map<Key, ActivityInvoker> activityInvokerMap() {
        Map<Key, ActivityInvoker> activityInvokerMap = Maps.newHashMap();
        for (Activities object : activities()) {
            for (Method method : object.getClass().getMethods()) {
                if (method != null && method.isAnnotationPresent(ActivityMethod.class)) {
                    ActivityMethod activityMethod = method.getAnnotation(ActivityMethod.class);
                    Key key = Key.of(activityMethod);
                    log.info(format("add activity %s", key));
                    ActivityInvoker activityInvoker = ImmutableActivityInvoker.builder().activityMethod(activityMethod)
                            .dataConverter(dataConverter()).instance(object).method(method)
                            .service(swf()).build();
                    activityInvokerMap.put(key,activityInvoker);
                }
            }
        }
        return activityInvokerMap;
    }

    /**
     * Register activities added to this poller on Amazon SWF with this instance's domain and task list.
     * {@link TypeAlreadyExistsException} are ignored making this method idempotent.
     *
     * @see ActivityMethod
     */
    @Override
    public void register() {
        for (ActivityInvoker invoker : activityInvokerMap().values()) {
            ActivityMethod method = invoker.activityMethod();
            Key key = Key.of(method);
            try {
                swf().registerActivityType(createRegisterActivityType(domain(), taskList(), method));
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
     * Each call performs a long polling or the next activity task from SWF and then calls
     * the matching registered {@link ActivityMethod} method to perform the task.
     * <p/>
     * <ul>
     * <li>Methods that succeed will cause a {@link RespondActivityTaskCompletedRequest} to be returned.</li>
     * <li>Methods that throw methods will cause a {@link RespondActivityTaskFailedRequest} to be returned.</li>
     * <li>Methods may issue zero or more {@link RecordActivityTaskHeartbeatRequest} calls while processing</li>
     * </ul>
     *
     */
    @Override
    protected void poll() {
        ActivityTask task = swf().pollForActivityTask(createPollForActivityTask(domain(), taskList(), name()));

        if (task.getTaskToken() == null) {
            return;
        }

        String input = task.getInput();
        Key key = Key.of(task.getActivityType());
        try {
            log.debug("start: {}", task);
            if (activityInvokerMap().containsKey(key)) {
                String result = activityInvokerMap().get(key).invoke(task);
                log.info("'{}' '{}' '{}' -> '{}'", task.getActivityId(), key, input, result);
                swf().respondActivityTaskCompleted(createRespondActivityCompleted(task, result));
            } else {
                String reason = format("Activity '%s' not registered on poller %s", task, name());
                log.error(reason);
                swf().respondActivityTaskFailed(
                        createRespondActivityTaskFailed(task.getTaskToken(), reason, null)
                );
            }
        } catch (Exception e) {
            log.error("'{}' '{}' '{}'", task.getActivityId(), key, input, e);
            String details = dataConverter().toData(e);
            swf().respondActivityTaskFailed(
                    createRespondActivityTaskFailed(task.getTaskToken(), e.getMessage(), details)
            );
        }
    }



    public static RegisterActivityTypeRequest createRegisterActivityType(Domain domain, TaskListName taskList, ActivityMethod method) {
        return new RegisterActivityTypeRequest()
                .withDomain(domain.value())
                .withDefaultTaskList(new TaskList().withName(taskList.value()))
                .withName(method.name())
                .withVersion(method.version())
                .withDescription(defaultIfEmpty(method.description(), null))
                .withDefaultTaskHeartbeatTimeout(defaultIfEmpty(method.heartbeatTimeout(), SWF_TIMEOUT_NONE))
                .withDefaultTaskStartToCloseTimeout(defaultIfEmpty(method.startToCloseTimeout(), SWF_TIMEOUT_NONE))
                .withDefaultTaskScheduleToStartTimeout(defaultIfEmpty(method.scheduleToStartTimeout(), SWF_TIMEOUT_NONE))
                .withDefaultTaskScheduleToCloseTimeout(defaultIfEmpty(method.scheduleToCloseTimeout(), SWF_TIMEOUT_NONE));
    }

    public static PollForActivityTaskRequest createPollForActivityTask(Domain domain, TaskListName taskList, Name name) {
        return new PollForActivityTaskRequest()
                .withDomain(domain.value())
                .withTaskList(new TaskList()
                        .withName(taskList.value()))
                .withIdentity(name.value());
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
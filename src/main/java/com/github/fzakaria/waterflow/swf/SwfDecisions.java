package com.github.fzakaria.waterflow.swf;

import com.amazonaws.services.simpleworkflow.model.ActivityType;
import com.amazonaws.services.simpleworkflow.model.Decision;
import com.amazonaws.services.simpleworkflow.model.DecisionType;
import com.amazonaws.services.simpleworkflow.model.RecordMarkerDecisionAttributes;
import com.amazonaws.services.simpleworkflow.model.ScheduleActivityTaskDecisionAttributes;
import com.amazonaws.services.simpleworkflow.model.StartTimerDecisionAttributes;
import com.amazonaws.services.simpleworkflow.model.TaskList;
import com.github.fzakaria.waterflow.immutable.ActionId;
import com.github.fzakaria.waterflow.immutable.Control;
import com.github.fzakaria.waterflow.immutable.Details;
import com.github.fzakaria.waterflow.immutable.Name;
import com.github.fzakaria.waterflow.immutable.TaskListName;
import com.github.fzakaria.waterflow.immutable.Version;
import org.immutables.builder.Builder;
import org.immutables.value.Value;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.util.Optional;

@Value.Style(newBuilder = "builder")
public class SwfDecisions {

    /**
     * @return decision of type {@link DecisionType#ScheduleActivityTask}
     */
    @Builder.Factory
    public static Decision scheduleActivityTaskDecision(
            @Nonnull ActionId actionId,
            @Nonnull Name name,
            @Nonnull Version version,
            Optional<String> input,
            Optional<Control> control,
            Optional<TaskListName> taskListName,
            Optional<String> heartbeatTimeout,
            Optional<String> scheduleToCloseTimeout,
            Optional<String> scheduleToStartTimeout,
            Optional<String> startToCloseTimeout,
            Optional<Integer> taskPriority) {
        TaskList taskList = taskListName.map(TaskListName::value)
                .map(t -> new TaskList().withName(t)).orElse(null);
        String taskPriorityString = taskPriority.map(String::valueOf).orElse(null);
        return new Decision()
                .withDecisionType(DecisionType.ScheduleActivityTask)
                .withScheduleActivityTaskDecisionAttributes(new ScheduleActivityTaskDecisionAttributes()
                        .withActivityType(new ActivityType()
                                .withName(name.value())
                                .withVersion(version.value()))
                        .withActivityId(actionId.value())
                        .withTaskList(taskList)
                        .withInput(input.orElse(null))
                        .withControl(control.map(Control::value).orElse(null))
                        .withHeartbeatTimeout(heartbeatTimeout.orElse(null))
                        .withScheduleToCloseTimeout(scheduleToCloseTimeout.orElse(null))
                        .withScheduleToStartTimeout(scheduleToStartTimeout.orElse(null))
                        .withStartToCloseTimeout(startToCloseTimeout.orElse(null))
                        .withTaskPriority(taskPriorityString));
    }

    /**
     * @return decision of type {@link DecisionType#RecordMarker}
     */
    @Builder.Factory
    public static Decision recordMarkerDecision(
            @Nonnull ActionId actionId,
            Optional<Details> details) {
        return new Decision()
                .withDecisionType(DecisionType.RecordMarker)
                .withRecordMarkerDecisionAttributes(new RecordMarkerDecisionAttributes()
                        .withMarkerName(actionId.value())
                        .withDetails(details.map(Details::value).orElse(null)));
    }

    /**
     * Create SWF {@link DecisionType#StartTimer} {@link Decision}.
     */
    @Builder.Factory
    public static Decision startTimerDecision(
            @Nonnull ActionId actionId,
            @Nonnull Duration startToFireTimeout,
            Optional<Control> control) {
        return new Decision()
                .withDecisionType(DecisionType.StartTimer)
                .withStartTimerDecisionAttributes(new StartTimerDecisionAttributes()
                        .withTimerId(actionId.value())
                        .withStartToFireTimeout(String.valueOf(startToFireTimeout.getSeconds()))
                        .withControl(control.map(Control::value).orElse(null)));
    }
}

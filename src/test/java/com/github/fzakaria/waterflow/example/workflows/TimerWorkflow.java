package com.github.fzakaria.waterflow.example.workflows;

import com.github.fzakaria.waterflow.Workflow;
import com.github.fzakaria.waterflow.action.ImmutableActivityActions;
import com.github.fzakaria.waterflow.action.ImmutableTimerAction;
import com.github.fzakaria.waterflow.immutable.ActionId;
import com.github.fzakaria.waterflow.immutable.Control;
import com.github.fzakaria.waterflow.immutable.DecisionContext;
import com.github.fzakaria.waterflow.immutable.Name;
import com.github.fzakaria.waterflow.immutable.Version;
import com.google.common.reflect.TypeToken;
import org.immutables.value.Value;

import java.time.Duration;
import java.util.concurrent.CompletionStage;

/**
 * A sample workflow showing how you might use a Timer to delay the processing of a certain activity.
 * This is the correct way to delay execution in the decider as it is "deterministic".
 * Do not use {@link Thread#sleep(long)} or any other such blocking activities
 */
@Value.Immutable
public abstract class TimerWorkflow extends Workflow<Integer, Integer> {

    public static final ActionId TIMER_NAME = ActionId.of("MY AWESOME TIMER");

    @Override
    public Name name() {
        return Name.of("Timer Workflow");
    }

    @Override
    public Version version() {
        return Version.of("1.0");
    }

    @Override
    public TypeToken<Integer> inputType() {
        return TypeToken.of(Integer.class);
    }

    @Override
    public TypeToken<Integer> outputType() {
        return TypeToken.of(Integer.class);
    }

    // Create known actions as fields
    final ImmutableActivityActions.IntegerActivityAction step1 = ImmutableActivityActions.IntegerActivityAction.builder().actionId(ActionId.of("step1"))
            .name(Name.of("Addition")).version(Version.of("1.0")).workflow(this).build();

    final ImmutableActivityActions.IntegerActivityAction step2 = ImmutableActivityActions.IntegerActivityAction.builder().actionId(ActionId.of("step2"))
            .name(Name.of("Addition")).version(Version.of("1.0")).workflow(this).build();

    final ImmutableTimerAction timerAction = ImmutableTimerAction.builder().actionId(TIMER_NAME)
            .workflow(this).startToFireTimeout(Duration.ofSeconds(5)).build();



    @Override
    public CompletionStage<Integer> decide(DecisionContext decisionContext) {
        // Set a breakpoint below to watch the decisions list to see what gets added on each call to Workflow.decide()
        CompletionStage<Integer> input = workflowInput(decisionContext.events());

        return input
                .thenCompose(i -> step1.withInput(i, 1).decide(decisionContext))
                .thenCompose(step1i -> {
                    Control control = Control.of(String.valueOf(step1i));
                    return timerAction.withControl(control).decide(decisionContext).thenCompose(v ->
                            step2.withInput(step1i, 100).decide(decisionContext));
                });
    }
}
package com.github.fzakaria.waterflow.example.workflows;

import com.github.fzakaria.waterflow.Workflow;
import com.github.fzakaria.waterflow.immutable.ActionId;
import com.github.fzakaria.waterflow.immutable.DecisionContext;
import com.github.fzakaria.waterflow.immutable.Name;
import com.github.fzakaria.waterflow.immutable.Version;
import com.google.common.reflect.TypeToken;
import org.immutables.value.Value;

import java.util.concurrent.CompletionStage;

import static com.github.fzakaria.waterflow.action.ImmutableActivityActions.IntegerActivityAction;

/**
 * WaterFlow "Hello World" example workflow that does three activities one after the other and then completes.
 */
@Value.Immutable
public abstract class SimpleWorkflow extends Workflow<Integer, Integer> {

    @Override
    public Name name() {
        return Name.of("Simple Workflow");
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
    final IntegerActivityAction step1 = IntegerActivityAction.builder().actionId(ActionId.of("step1"))
            .name(Name.of("Addition")).version(Version.of("1.0")).workflow(this).build();

    final IntegerActivityAction step2 = IntegerActivityAction.builder().actionId(ActionId.of("step2"))
            .name(Name.of("Addition")).version(Version.of("1.0")).workflow(this).build();

    final IntegerActivityAction step3 = IntegerActivityAction.builder().actionId(ActionId.of("step3"))
            .name(Name.of("Addition")).version(Version.of("1.0")).workflow(this).build();


    @Override
    public CompletionStage<Integer> decide(DecisionContext decisionContext) {
        // Set a breakpoint below to watch the decisions list to see what gets added on each call to Workflow.decide()
        CompletionStage<Integer> input = workflowInput(decisionContext.events());

        return input
                .thenCompose(i -> step1.withInput(i, 1).decide(decisionContext))
                .thenCompose(step1i -> step2.withInput(step1i, 100).decide(decisionContext))
                .thenCompose(step2i -> step3.withInput(step2i, 100).decide(decisionContext));

    }
}
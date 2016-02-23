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
 * This is a sample workflow that demonstrates how you can throw Throwables
 * <b>across</b> activities and have them propogated to the decider.
 */
@Value.Immutable
public abstract class ThrowingWorkflow extends Workflow<Integer, Integer>  {

    @Override
    public Name name() {
        return Name.of("Throwing Workflow");
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
            .name(Name.of("Division")).version(Version.of("1.0")).workflow(this).build();

    @Override
    public CompletionStage<Integer> decide(DecisionContext decisionContext) {
        CompletionStage<Integer> input = workflowInput(decisionContext.events());

        return input.thenCompose(i -> step1.withInput(i, 0).decide(decisionContext));
    }
}


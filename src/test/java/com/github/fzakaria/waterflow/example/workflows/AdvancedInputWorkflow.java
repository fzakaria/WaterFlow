package com.github.fzakaria.waterflow.example.workflows;

import com.github.fzakaria.waterflow.Workflow;
import com.github.fzakaria.waterflow.immutable.ActionId;
import com.github.fzakaria.waterflow.immutable.DecisionContext;
import com.github.fzakaria.waterflow.immutable.Name;
import com.github.fzakaria.waterflow.immutable.Version;
import com.google.common.reflect.TypeToken;
import org.immutables.value.Value;

import java.util.concurrent.CompletionStage;

import static com.github.fzakaria.waterflow.action.ActivityActions.VoidActivityAction;


/**
 * WaterFlow Advanced 'Hello World' Sample.
 * This is very similar to {@link SimpleWorkflow} except it demonstrates the power of
 * serializing & deserializing complex POJOs and activities that even take more than 1 parameter.
 * <p>
 * Step 1 - Is an example of how an Activity may take more than 1 input of a complex type
 * Step 2 - Demonstrates passing a complex type'd Collection and returning Void
 */

@Value.Immutable
public abstract class AdvancedInputWorkflow extends Workflow<AdamAndEve, AdamAndEve> {

    @Override
    public Name name() {
        return Name.of("Advanced Workflow");
    }

    @Override
    public Version version() {
        return Version.of("1.0");
    }

    @Override
    public TypeToken<AdamAndEve> inputType() {
        return TypeToken.of(AdamAndEve.class);
    }

    @Override
    public TypeToken<AdamAndEve> outputType() {
        return TypeToken.of(AdamAndEve.class);
    }

    // Create known actions as fields
    final AnimalActivityAction step1 = AnimalActivityAction.builder().actionId(ActionId.of("step1"))
            .name(Name.of("Mate")).version(Version.of("1.0")).workflow(this).build();

    final VoidActivityAction step2 = VoidActivityAction.builder().actionId(ActionId.of("step2"))
            .name(Name.of("Echo")).version(Version.of("1.0")).workflow(this).build();


    @Override
    public CompletionStage<AdamAndEve> decide(DecisionContext decisionContext) {
        // Set a breakpoint below to watch the decisions list to see what gets added on each call to Workflow.decide()
        CompletionStage<AdamAndEve> input = workflowInput(decisionContext.events());

        CompletionStage<Animal> step1CompletionStage =
                input.thenCompose(i -> step1.withInput(i.adam(), i.eve()).decide(decisionContext));

        CompletionStage<Void> step2CompletionStage =
                step1CompletionStage.thenCompose(child ->
                input.thenCompose(i ->
                        step2.withInput((Object) new Animal[]{i.adam(), i.eve(), child}).decide(decisionContext)));

        //once the echo activity has completed lets return the original input (for testing)
        return step2CompletionStage.thenCombine(input, (v, i) -> i);
    }
}

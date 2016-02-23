package com.github.fzakaria.waterflow.example.workflows;

import com.github.fzakaria.waterflow.Workflow;
import com.github.fzakaria.waterflow.action.ImmutableRecordMarkerAction;
import com.github.fzakaria.waterflow.immutable.ActionId;
import com.github.fzakaria.waterflow.immutable.DecisionContext;
import com.github.fzakaria.waterflow.immutable.Details;
import com.github.fzakaria.waterflow.immutable.Name;
import com.github.fzakaria.waterflow.immutable.Version;
import com.google.common.reflect.TypeToken;
import org.immutables.value.Value;

import java.util.concurrent.CompletionStage;

import static com.github.fzakaria.waterflow.action.ImmutableActivityActions.IntegerActivityAction;

/**
 * This is a very simple demonstration of how you can record
 * a marker in your workflow.
 * Markers are used to help diagnose issues or perform more complicated decider
 * logic.
 */
@Value.Immutable
public abstract class SimpleMarkerWorkflow extends Workflow<Integer, Integer> {

    public static final ActionId MARKER_NAME = ActionId.of("MY AWESOME MARKER");

    @Override
    public Name name() {
        return Name.of("Simple Marker Workflow");
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

    final ImmutableRecordMarkerAction recordMarkerAction = ImmutableRecordMarkerAction.builder().actionId(MARKER_NAME)
            .workflow(this).build();

    @Override
    public CompletionStage<Integer> decide(DecisionContext decisionContext) {
        // Set a breakpoint below to watch the decisions list to see what gets added on each call to Workflow.decide()
        CompletionStage<Integer> input = workflowInput(decisionContext.events());

        return input
                .thenCompose(i -> step1.withInput(i, 1).decide(decisionContext))
                .thenCompose(step1i -> {
                    final Details details = Details.of(String.valueOf(step1i));
                    return recordMarkerAction.withDetails(details).decide(decisionContext).thenCompose( v ->
                            step2.withInput(step1i, 100).decide(decisionContext));

                });

    }
}
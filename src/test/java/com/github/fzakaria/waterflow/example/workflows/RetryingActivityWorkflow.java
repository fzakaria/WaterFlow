package com.github.fzakaria.waterflow.example.workflows;

import com.github.fzakaria.waterflow.Workflow;
import com.github.fzakaria.waterflow.action.ActivityActions;
import com.github.fzakaria.waterflow.immutable.ActionId;
import com.github.fzakaria.waterflow.immutable.DecisionContext;
import com.github.fzakaria.waterflow.immutable.Name;
import com.github.fzakaria.waterflow.immutable.Version;
import com.github.fzakaria.waterflow.retry.FixedDelayRetryStrategy;
import com.google.common.reflect.TypeToken;
import org.immutables.value.Value;

import java.time.Duration;
import java.util.concurrent.CompletionStage;

/**
 * A sample workflow that demonstrates activity that has a retry strategy.
 */
@Value.Immutable
public abstract class RetryingActivityWorkflow extends Workflow<Integer, Integer> {

    @Override
    public Name name() {
        return Name.of("Retrying Workflow");
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
    final ActivityActions.IntegerActivityAction step1 = ActivityActions.IntegerActivityAction.builder()
            .actionId(ActionId.of("step1")).retryStrategy(new FixedDelayRetryStrategy(Duration.ofSeconds(3)))
            .name(Name.of("PassesModuloThree")).version(Version.of("1.0")).workflow(this).build();

    @Override
    public CompletionStage<Integer> decide(DecisionContext decisionContext) {
        // Set a breakpoint below to watch the decisions list to see what gets added on each call to Workflow.decide()
        CompletionStage<Integer> input = workflowInput(decisionContext.events());
        return input.thenCompose(i ->  step1.withInput(i).decide(decisionContext));
    }
}
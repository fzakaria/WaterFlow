package com.github.fzakaria.waterflow.example.workflows;

import com.github.fzakaria.waterflow.Workflow;
import com.github.fzakaria.waterflow.action.ImmutableWaitSignalAction;
import com.github.fzakaria.waterflow.action.WaitSignalAction;
import com.github.fzakaria.waterflow.immutable.ActionId;
import com.github.fzakaria.waterflow.immutable.DecisionContext;
import com.github.fzakaria.waterflow.immutable.Name;
import com.github.fzakaria.waterflow.immutable.Version;
import com.google.common.reflect.TypeToken;
import org.immutables.value.Value;

import java.util.concurrent.CompletionStage;

import static com.github.fzakaria.waterflow.action.ActivityActions.VoidActivityAction;

/**
 * A sample workflow showing how you might write a workflow to wait for a signal either
 * sent externally or from another Workflow
 */
@Value.Immutable
public abstract class WaitForSignalWorkflow extends Workflow<Void, String> {

    public static final ActionId SIGNAL_NAME = ActionId.of("MY AWESOME SIGNAL");

    @Override
    public Name name() {
        return Name.of("Wait Signal Workflow");
    }

    @Override
    public Version version() {
        return Version.of("1.0");
    }

    @Override
    public TypeToken<Void> inputType() {
        return TypeToken.of(Void.class);
    }

    @Override
    public TypeToken<String> outputType() {
        return TypeToken.of(String.class);
    }

    // Create known actions as fields
    final VoidActivityAction step1 = VoidActivityAction.builder().actionId(ActionId.of("step1"))
            .name(Name.of("Echo")).version(Version.of("1.0")).workflow(this).build();

    final WaitSignalAction signal = ImmutableWaitSignalAction.builder().actionId(SIGNAL_NAME)
            .workflow(this).build();


    @Override
    public CompletionStage<String> decide(DecisionContext decisionContext) {
        CompletionStage<String> signalResult = signal.decide(decisionContext);
        CompletionStage<Void> tasks = signalResult.thenCompose(s -> step1.withInput((Object)new String[]{s}).decide(decisionContext));
        return tasks.thenCompose(t -> signalResult);
    }

}

package com.github.fzakaria.waterflow.example.workflows;

import com.github.fzakaria.waterflow.Workflow;
import com.github.fzakaria.waterflow.action.ImmutableActivityActions;
import com.github.fzakaria.waterflow.immutable.ActionId;
import com.github.fzakaria.waterflow.immutable.DecisionContext;
import com.github.fzakaria.waterflow.immutable.Name;
import com.github.fzakaria.waterflow.immutable.Version;
import com.google.common.reflect.TypeToken;
import org.immutables.value.Value;

import java.util.concurrent.CompletionStage;

/**
 * WaterFlow example workflow that demonstrates an activity which makes use of "recordHeartbeat"
 * @see ExampleActivities#recordHeartbeat(String)
 */
@Value.Immutable
public abstract class HeartbeatWorkflow extends Workflow<Void, Void> {

    @Override
    public Name name() {
        return Name.of("Heartbeat Workflow");
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
    public TypeToken<Void> outputType() {
        return TypeToken.of(Void.class);
    }

    // Create known actions as fields
    final ImmutableActivityActions.VoidActivityAction step1 = ImmutableActivityActions.VoidActivityAction.builder().actionId(ActionId.of("step1"))
            .name(Name.of("Heartbeat")).version(Version.of("1.0")).workflow(this).build();



    @Override
    public CompletionStage<Void> decide(DecisionContext decisionContext) {
        // Set a breakpoint below to watch the decisions list to see what gets added on each call to Workflow.decide()
        return step1.decide(decisionContext);

    }
}
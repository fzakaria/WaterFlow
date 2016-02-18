package com.github.fzakaria.waterflow.example.workflows;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.github.fzakaria.waterflow.Workflow;
import com.github.fzakaria.waterflow.action.ActivityAction;
import com.github.fzakaria.waterflow.example.Config;
import com.google.common.reflect.TypeToken;
import lombok.Data;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;

import static com.amazonaws.services.simpleworkflow.model.ChildPolicy.TERMINATE;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * SWiFt Advanced 'Hello World' Sample.
 * This is very similar to {@link SimpleWorkflow} except it demonstrates the power of
 * serializing & deserializing complex POJOs and activities that even take more than 1 parameter.
 *
 * Step1 - Is an example of how an Activity may take more than 1 input of a complex type
 * Step 2 - Demonstrates passing a complex type'd Collection and returning Void
 */
public class AdvancedInputWorkflow extends Workflow<AdvancedInputWorkflow.AdamAndEve, Void> {

    @Data
    public static class AdamAndEve {
        public Animal adam;
        public Animal eve;
        public AdamAndEve() {

        }
    }

    public static void main(String[] args) {
        Config config = new Config();
        Workflow<AdvancedInputWorkflow.AdamAndEve, Void> workflow = new AdvancedInputWorkflow()
                .domain(config.getDomain())
                .taskList(config.getTaskList())
                .executionStartToCloseTimeout(MINUTES, 5)
                .taskStartToCloseTimeout(SECONDS, 30)
                .childPolicy(TERMINATE)
                .description("An Advanced Example Workflow ");

        Animal adam = new Animal();
        adam.setWeight(44.0);
        Animal eve = new Animal();
        adam.setWeight(34.0);
        AdamAndEve adamAndEve = new AdamAndEve();
        adamAndEve.adam = adam;
        adamAndEve.eve = eve;
        config.submit(workflow, adamAndEve);
    }

    // Create known actions as fields
    final ActivityAction<Animal> step1 = new ActivityAction<>("step1", "Mate", "1.0", Animal.class);
    final ActivityAction<Void> step2 = new ActivityAction<>("step3", "Echo", "1.0", Void.class);

    /** Start the workflow by submitting it to SWF. */
    public AdvancedInputWorkflow() {
        super("Advanced Workflow", "1.0", AdamAndEve.class, Void.class);

        // This step registers the steps with the workflow so that you don't manually have to
        // inject their workflow, history, state with each call to decide()
        actions(step1, step2);
    }

    @Override
        public CompletionStage<Void> decide(List<Decision> decisions) {
        // Set a breakpoint below to watch the decisions list to see what gets added on each call to Workflow.decide()
        CompletionStage<AdamAndEve> input = workflowInput();

        CompletionStage<Animal> step1CompletionStage =
                input.thenCompose(i -> step1.input(i.adam, i.eve).decide(decisions));

        return step1CompletionStage.thenCompose( child ->
                input.thenCompose( i ->
                        step2.input((Object)new Animal[]{i.adam, i.eve, child}).decide(decisions)));


    }
}
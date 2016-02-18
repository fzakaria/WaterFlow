package com.github.fzakaria.waterflow.example.workflows;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.github.fzakaria.waterflow.Workflow;
import com.github.fzakaria.waterflow.action.ActivityAction;
import com.github.fzakaria.waterflow.example.Config;

import java.util.List;
import java.util.concurrent.CompletionStage;

import static com.amazonaws.services.simpleworkflow.model.ChildPolicy.TERMINATE;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * SWiFt "Hello World" example workflow that does three activities one after the other and then completes.
 */
public class SimpleWorkflow extends Workflow<Integer, Integer> {

    public static void main(String[] args) {
        Config config = new Config();
        Workflow<Integer, Integer> workflow = new SimpleWorkflow()
                .domain(config.getDomain())
                .taskList(config.getTaskList())
                .executionStartToCloseTimeout(MINUTES, 5)
                .taskStartToCloseTimeout(SECONDS, 30)
                .childPolicy(TERMINATE)
                .description("A Simple Example Workflow");
        config.submit(workflow, 100);
    }

    // Create known actions as fields
    final ActivityAction<Integer> step1 = new ActivityAction<>("step1", "Addition", "1.0", Integer.class);
    final ActivityAction<Integer> step2 = new ActivityAction<>("step2", "Addition", "1.0", Integer.class);
    final ActivityAction<Integer> step3 = new ActivityAction<>("step3", "Addition", "1.0", Integer.class);


    /** Start the workflow by submitting it to SWF. */
    public SimpleWorkflow() {
        super("Simple Workflow", "1.0", Integer.class, Integer.class);

        // This step registers the steps with the workflow so that you don't manually have to
        // inject their workflow, history, state with each call to decide()
        actions(step1, step2, step3);
    }

    @Override
    public CompletionStage<Integer> decide(List<Decision> decisions) {
        // Set a breakpoint below to watch the decisions list to see what gets added on each call to Workflow.decide()
        CompletionStage<Integer> input = workflowInput();
        return input
                .thenCompose(i -> step1.input(i,1).decide(decisions))
                .thenCompose(step1i -> step2.input(step1i, 100).decide(decisions))
                .thenCompose(step2i -> step3.input(step2i, 100).decide(decisions));

    }
}
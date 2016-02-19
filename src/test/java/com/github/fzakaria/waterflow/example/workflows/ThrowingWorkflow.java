/*
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

*/
/**
 * This is a sample workflow that demonstrates how you can throw Throwables
 * <b>across</b> activities and have them propogated to the decider.
 *//*

public class ThrowingWorkflow extends Workflow<Integer, Integer>  {

    public static void main(String[] args) {
        Config config = new Config();
        Workflow<Integer, Integer> workflow = new ThrowingWorkflow()
                .domain(config.getDomain())
                .taskList(config.getTaskList())
                .executionStartToCloseTimeout(MINUTES, 5)
                .taskStartToCloseTimeout(SECONDS, 30)
                .childPolicy(TERMINATE)
                .description("A Throwing Example Workflow");
        config.submit(workflow, 100);
    }

    // Create known actions as fields
    final ActivityAction<Integer> step1 = new ActivityAction<>("step1", "Activity X", "1.0", Integer.class);
    final ActivityAction<Integer> step2 = new ActivityAction<>("step2", "Activity Y", "1.0", Integer.class);
    final ActivityAction<Integer> step3 = new ActivityAction<>("step3", "Activity Z", "1.0", Integer.class);


    */
/** Start the workflow by submitting it to SWF. *//*

    public ThrowingWorkflow() {
        super("ThrowingWorkflow Workflow", "1.0", Integer.class, Integer.class);

        // This step registers the steps with the workflow so that you don't manually have to
        // inject their workflow, history, state with each call to decide()
        actions(step1, step2, step3);
    }

    @Override
    public CompletionStage<Integer> decide(List<Decision> decisions) {
        return null;
    }
}
*/

package com.github.fzakaria.waterflow.example;

import com.amazonaws.services.simpleworkflow.flow.common.WorkflowExecutionUtils;
import com.amazonaws.services.simpleworkflow.model.TerminateWorkflowExecutionRequest;
import com.amazonaws.services.simpleworkflow.model.UnknownResourceException;
import com.amazonaws.services.simpleworkflow.model.WorkflowExecution;
import com.github.fzakaria.waterflow.TerminateWorkflowRequestBuilder;
import com.github.fzakaria.waterflow.Workflow;
import com.github.fzakaria.waterflow.example.workflows.ImmutableSimpleWorkflow;
import com.github.fzakaria.waterflow.immutable.Description;
import com.github.fzakaria.waterflow.immutable.Reason;
import com.github.fzakaria.waterflow.immutable.RunId;
import com.github.fzakaria.waterflow.immutable.WorkflowId;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static com.amazonaws.services.simpleworkflow.model.ChildPolicy.TERMINATE;
import static org.hamcrest.CoreMatchers.is;
import static com.jayway.awaitility.Awaitility.*;

public class ExamplesIntegrationTest {

    protected static final Logger log = LoggerFactory.getLogger(ActivityDecisionPollerPool.class);

    private static final Config config = ImmutableConfig.of();

    private static final ActivityDecisionPollerPool activityDecisionPollerPool
            = ImmutableActivityDecisionPollerPool.builder().config(config).build();

    private WorkflowExecution workflowExecution;

    @BeforeClass
    public static void beforeClass() {
        activityDecisionPollerPool.start();
    }

    @AfterClass
    public static void afterClass() {
        activityDecisionPollerPool.stop();
    }

    @After
    public void after() {
        if (workflowExecution == null) {
            log.debug("No workflow execution to cleanup.");
            return;
        }
        WorkflowId workflowId = WorkflowId.of(workflowExecution.getWorkflowId());
        RunId runId = RunId.of(workflowExecution.getRunId());

        TerminateWorkflowExecutionRequest request = TerminateWorkflowRequestBuilder.builder().domain(config.domain())
                .reason(Reason.of("Test cleanup")).workflowId(workflowId).runId(runId).build();
        try {
            log.debug("Requesting termination of {} - {}", workflowId, runId);
            config.swf().terminateWorkflowExecution(request);
        } catch (UnknownResourceException e) {
            log.debug("Already terminated.");
        } catch (Exception e) {
            log.debug("Unexpected error during termination.", e);
        }

    }

    @Test
    public void simpleWorkflowTest() {
        //submit workflow
        Workflow<Integer, Integer> workflow = ImmutableSimpleWorkflow.builder()
                .taskList(config.taskList())
                .executionStartToCloseTimeout(Duration.ofMinutes(5))
                .taskStartToCloseTimeout(Duration.ofSeconds(30))
                .childPolicy(TERMINATE)
                .description(Description.of("A Simple Example Workflow"))
                .dataConverter(config.dataConverter()).build();
        workflowExecution = config.submit(workflow, 100);
        await().ignoreExceptions().pollInterval(1, TimeUnit.SECONDS).atMost(30, TimeUnit.SECONDS).until(() ->
                        getWorkflowExecutionResult(workflowExecution),
                is("301"));
    }

    private static String getWorkflowExecutionResult(WorkflowExecution workflowExecution) {
        return WorkflowExecutionUtils.getWorkflowExecutionResult(config.swf(), config.domain().value(), workflowExecution).getResult();
    }

}

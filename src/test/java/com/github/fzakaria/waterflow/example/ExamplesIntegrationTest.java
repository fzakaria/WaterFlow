package com.github.fzakaria.waterflow.example;

import com.amazonaws.services.simpleworkflow.flow.common.WorkflowExecutionUtils;
import com.amazonaws.services.simpleworkflow.model.EventType;
import com.amazonaws.services.simpleworkflow.model.HistoryEvent;
import com.amazonaws.services.simpleworkflow.model.TerminateWorkflowExecutionRequest;
import com.amazonaws.services.simpleworkflow.model.UnknownResourceException;
import com.amazonaws.services.simpleworkflow.model.WorkflowExecution;
import com.github.fzakaria.waterflow.TaskType;
import com.github.fzakaria.waterflow.event.Event;
import com.github.fzakaria.waterflow.example.workflows.ImmutableHeartbeatWorkflow;
import com.github.fzakaria.waterflow.example.workflows.ImmutableSimpleMarkerWorkflow;
import com.github.fzakaria.waterflow.example.workflows.ImmutableTimerWorkflow;
import com.github.fzakaria.waterflow.example.workflows.SimpleMarkerWorkflow;
import com.github.fzakaria.waterflow.example.workflows.TimerWorkflow;
import com.github.fzakaria.waterflow.swf.TerminateWorkflowRequestBuilder;
import com.github.fzakaria.waterflow.Workflow;
import com.github.fzakaria.waterflow.example.workflows.AdamAndEve;
import com.github.fzakaria.waterflow.example.workflows.ImmutableAdvancedInputWorkflow;
import com.github.fzakaria.waterflow.example.workflows.ImmutableAnimal;
import com.github.fzakaria.waterflow.example.workflows.ImmutableSimpleWorkflow;
import com.github.fzakaria.waterflow.example.workflows.ImmutableThrowingWorkflow;
import com.github.fzakaria.waterflow.immutable.Description;
import com.github.fzakaria.waterflow.immutable.Reason;
import com.github.fzakaria.waterflow.immutable.RunId;
import com.github.fzakaria.waterflow.immutable.WorkflowId;
import com.google.common.collect.Iterables;
import org.hamcrest.text.IsEmptyString;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.amazonaws.services.simpleworkflow.model.ChildPolicy.TERMINATE;
import static com.jayway.awaitility.Awaitility.await;
import static java.util.stream.Collectors.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.isA;
import static org.hamcrest.text.IsEmptyString.*;
import static org.junit.Assert.assertThat;

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
        await().ignoreExceptions().pollInterval(1, TimeUnit.SECONDS).atMost(3, TimeUnit.MINUTES).until(() ->
                        getWorkflowExecutionResult(workflowExecution),
                is("301"));
    }

    @Test
    public void advancedWorkflowTest() {
        //submit workflow
        Workflow<AdamAndEve, AdamAndEve> workflow = ImmutableAdvancedInputWorkflow.builder()
                .taskList(config.taskList())
                .executionStartToCloseTimeout(Duration.ofMinutes(5))
                .taskStartToCloseTimeout(Duration.ofSeconds(30))
                .childPolicy(TERMINATE)
                .description(Description.of("A Advanced Example Workflow"))
                .dataConverter(config.dataConverter()).build();
        AdamAndEve adamAndEve = AdamAndEve.of(ImmutableAnimal.builder().build(),
                ImmutableAnimal.builder().build());
        String expectedResult = config.dataConverter().toData(adamAndEve);
        workflowExecution = config.submit(workflow, adamAndEve);
        await().ignoreExceptions().pollInterval(1, TimeUnit.SECONDS).atMost(3, TimeUnit.MINUTES).until(() ->
                        getWorkflowExecutionResult(workflowExecution),
                is(expectedResult));
    }

    @Test
    public void throwingWorkflowTest() {
        //submit workflow
        Workflow<Integer, Integer> workflow = ImmutableThrowingWorkflow.builder()
                .taskList(config.taskList())
                .executionStartToCloseTimeout(Duration.ofMinutes(5))
                .taskStartToCloseTimeout(Duration.ofSeconds(30))
                .childPolicy(TERMINATE)
                .description(Description.of("A Throwing Example Workflow"))
                .dataConverter(config.dataConverter()).build();
        workflowExecution = config.submit(workflow, 100);
        await().ignoreExceptions().pollInterval(1, TimeUnit.SECONDS).atMost(3, TimeUnit.MINUTES).until(() -> {
                    String result = getWorkflowExecutionResult(workflowExecution);
                    return config.dataConverter().fromData(result, Throwable.class);
                },
                isA(ArithmeticException.class));
    }

    @Test
    public void markerWorkflowTest() {
        //submit workflow
        Workflow<Integer, Integer> workflow = ImmutableSimpleMarkerWorkflow.builder()
                .taskList(config.taskList())
                .executionStartToCloseTimeout(Duration.ofMinutes(5))
                .taskStartToCloseTimeout(Duration.ofSeconds(30))
                .childPolicy(TERMINATE)
                .description(Description.of("A Marker Example Workflow"))
                .dataConverter(config.dataConverter()).build();
        workflowExecution = config.submit(workflow, 100);
        await().ignoreExceptions().pollInterval(1, TimeUnit.SECONDS).atMost(3, TimeUnit.MINUTES).until(() ->
                        getWorkflowExecutionResult(workflowExecution),
                is("201"));
        List<HistoryEvent> historyEvents = WorkflowExecutionUtils.getHistory(config.swf(), config.domain().value(), workflowExecution);
        List<Event> events = Event.fromHistoryEvents(historyEvents);
        List<Event> recordMarkerEvents = events.stream()
                .filter(event -> event.task() == TaskType.RECORD_MARKER).collect(toList());
        assertThat("incorrect number of marker events", recordMarkerEvents, hasSize(1));
        Event recordMarkerEvent = Iterables.getOnlyElement(recordMarkerEvents);
        assertThat(recordMarkerEvent.actionId(), is(SimpleMarkerWorkflow.MARKER_NAME));
        assertThat(recordMarkerEvent.details(), is("101"));
    }

    @Test
    public void timerWorkflowTest() {
        //submit workflow
        Workflow<Integer, Integer> workflow = ImmutableTimerWorkflow.builder()
                .taskList(config.taskList())
                .executionStartToCloseTimeout(Duration.ofMinutes(5))
                .taskStartToCloseTimeout(Duration.ofSeconds(30))
                .childPolicy(TERMINATE)
                .description(Description.of("A Timer Example Workflow"))
                .dataConverter(config.dataConverter()).build();
        workflowExecution = config.submit(workflow, 100);
        await().ignoreExceptions().pollInterval(1, TimeUnit.SECONDS).atMost(3, TimeUnit.MINUTES).until(() ->
                        getWorkflowExecutionResult(workflowExecution),
                is("201"));
        List<HistoryEvent> historyEvents = WorkflowExecutionUtils.getHistory(config.swf(), config.domain().value(), workflowExecution);
        List<Event> events = Event.fromHistoryEvents(historyEvents);
        List<Event> timerEvents = events.stream()
                .filter(event -> event.task() == TaskType.TIMER).collect(toList());
        //TimerStarted & TimerFired
        assertThat("incorrect number of marker events", timerEvents, hasSize(2));
        Optional<Event> timerEvent = timerEvents.stream().filter(e -> e.type() == EventType.TimerStarted).findFirst();
        assertThat(timerEvent.get().actionId(), is(TimerWorkflow.TIMER_NAME));
        assertThat(timerEvent.get().control(), is("101"));
    }

    @Test
    public void heartbeatsWorkflowTest() {
        //submit workflow
        Workflow<Void, Void> workflow = ImmutableHeartbeatWorkflow.builder()
                .taskList(config.taskList())
                .executionStartToCloseTimeout(Duration.ofMinutes(5))
                .taskStartToCloseTimeout(Duration.ofSeconds(30))
                .childPolicy(TERMINATE)
                .description(Description.of("A Heartbeat Example Workflow"))
                .dataConverter(config.dataConverter()).build();
        workflowExecution = config.submit(workflow, null);
        await().ignoreExceptions().pollInterval(1, TimeUnit.SECONDS).atMost(3, TimeUnit.MINUTES).until(() ->
                        getWorkflowExecutionResult(workflowExecution),
                isEmptyOrNullString());
    }

    private static String getWorkflowExecutionResult(WorkflowExecution workflowExecution) {
        return WorkflowExecutionUtils.getWorkflowExecutionResult(config.swf(), config.domain().value(), workflowExecution).getResult();
    }

}

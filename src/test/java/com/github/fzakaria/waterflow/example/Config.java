package com.github.fzakaria.waterflow.example;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowClient;
import com.amazonaws.services.simpleworkflow.model.Run;
import com.amazonaws.services.simpleworkflow.model.StartWorkflowExecutionRequest;
import com.amazonaws.services.simpleworkflow.model.WorkflowExecution;
import com.github.fzakaria.waterflow.Workflow;
import com.github.fzakaria.waterflow.converter.DataConverter;
import com.github.fzakaria.waterflow.converter.ImmutableJacksonDataConverter;
import com.github.fzakaria.waterflow.immutable.Domain;
import com.github.fzakaria.waterflow.immutable.Input;
import com.github.fzakaria.waterflow.immutable.TaskListName;
import com.github.fzakaria.waterflow.immutable.WorkflowId;
import com.github.fzakaria.waterflow.swf.WorkflowExecutionRequestBuilder;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Optional;

import static java.lang.String.format;

@Value.Immutable(singleton = true)
public abstract class Config {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Value.Default
    public DataConverter dataConverter() {
       return ImmutableJacksonDataConverter.builder().build();
    }

    @Value.Default
    public AmazonSimpleWorkflow swf() {
        //SWF holds the connection for 60 seconds to see if a decision is available
        final Duration DEFAULT_CONNECTION_TIMEOUT = Duration.ofSeconds(60);
        final Duration DEFAULT_SOCKET_TIMEOUT = DEFAULT_CONNECTION_TIMEOUT.plusSeconds(10);
        return new AmazonSimpleWorkflowClient(new DefaultAWSCredentialsProviderChain(),
                new ClientConfiguration().withConnectionTimeout((int) DEFAULT_CONNECTION_TIMEOUT.toMillis())
                        .withSocketTimeout((int) DEFAULT_SOCKET_TIMEOUT.toMillis()));
    }

    @Value.Default
    public Domain domain() {
        return Domain.of("swift");
    }

    @Value.Default
    public TaskListName taskList() {
        return TaskListName.of("DEFAULT");
    }

    @Value.Default
    public int activityPoolSize() {
        return 2;
    }

    @Value.Default
    public int decisionPoolSize() {
        return 2;
    }


    public WorkflowExecution submit(Workflow workflow, WorkflowId workflowId, Optional<Object> input) {
        log.info(format("submit workflow: %s", workflowId));

        Optional<Input> inputOptional = input.map( i -> dataConverter().toData(i)).map(Input::of);

        StartWorkflowExecutionRequest request =
                WorkflowExecutionRequestBuilder.builder().domain(domain())
                .workflow(workflow).input(inputOptional)
                .taskList(taskList()).workflowId(workflowId).build();

        log.info(format("Start workflow execution: %s", workflowId));
        Run run = swf().startWorkflowExecution(request);
        log.info(format("Started workflow %s", run));
        return new WorkflowExecution().withWorkflowId(workflowId.value()).withRunId(run.getRunId());
    }

    public <I, O> WorkflowExecution submit(Workflow<I,O> workflow, I input) {
        WorkflowId workflowId = WorkflowId.randomUniqueWorkflowId(workflow);
        return submit(workflow, workflowId, Optional.ofNullable(input));
    }
}
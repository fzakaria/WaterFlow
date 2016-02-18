package com.github.fzakaria.waterflow.example;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowClient;
import com.amazonaws.services.simpleworkflow.model.Run;
import com.amazonaws.services.simpleworkflow.model.StartWorkflowExecutionRequest;
import com.amazonaws.services.simpleworkflow.model.WorkflowExecution;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fzakaria.waterflow.SwfUtil;
import com.github.fzakaria.waterflow.Workflow;
import com.github.fzakaria.waterflow.converter.DataConverter;
import com.github.fzakaria.waterflow.converter.JacksonDataConverter;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

import static java.lang.String.format;

@Slf4j
@Value
public class Config {
    private final DataConverter dataConverter;
    private final AmazonSimpleWorkflow amazonSimpleWorkflow;
    private final String domain = "swift";
    private final String taskList = "DEFAULT";
    private final int activityPoolSize = 2;
    private final int decisionPoolSize = 2;

    public Config() {
        //SWF holds the connection for 60 seconds to see if a decision is available
        final Duration DEFAULT_CONNECTION_TIMEOUT = Duration.ofSeconds(60);
        final Duration DEFAULT_SOCKET_TIMEOUT = DEFAULT_CONNECTION_TIMEOUT.plusSeconds(10);
        amazonSimpleWorkflow = new AmazonSimpleWorkflowClient(new DefaultAWSCredentialsProviderChain(),
                new ClientConfiguration().withConnectionTimeout((int) DEFAULT_CONNECTION_TIMEOUT.toMillis())
                        .withSocketTimeout((int) DEFAULT_SOCKET_TIMEOUT.toMillis())
        );
        dataConverter = new JacksonDataConverter();
    }

    public WorkflowExecution submit(Workflow workflow, String workflowId, Object input) {
        log.info(format("submit workflow: %s", workflowId));

        workflow.addTags("Swift");
        String inputAsString = dataConverter.toData(input);
        StartWorkflowExecutionRequest request = workflow.createWorkflowExecutionRequest(workflowId, inputAsString);

        log.info(format("Start workflow execution: %s", workflowId));
        Run run = getAmazonSimpleWorkflow().startWorkflowExecution(request);
        log.info(format("Started workflow %s", run));
        return new WorkflowExecution().withWorkflowId(workflowId).withRunId(run.getRunId());
    }

    public <I, O> WorkflowExecution submit(Workflow<I,O> workflow, I input) {
        String workflowId = SwfUtil.createUniqueWorkflowId(workflow.name());
        return submit(workflow, workflowId, input);
    }
}
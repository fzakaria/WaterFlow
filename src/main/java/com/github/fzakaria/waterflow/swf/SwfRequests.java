package com.github.fzakaria.waterflow.swf;

import com.amazonaws.services.simpleworkflow.model.ChildPolicy;
import com.amazonaws.services.simpleworkflow.model.Decision;
import com.amazonaws.services.simpleworkflow.model.DecisionType;
import com.amazonaws.services.simpleworkflow.model.RecordMarkerDecisionAttributes;
import com.amazonaws.services.simpleworkflow.model.RegisterWorkflowTypeRequest;
import com.amazonaws.services.simpleworkflow.model.StartTimerDecisionAttributes;
import com.amazonaws.services.simpleworkflow.model.StartWorkflowExecutionRequest;
import com.amazonaws.services.simpleworkflow.model.TaskList;
import com.amazonaws.services.simpleworkflow.model.TerminateWorkflowExecutionRequest;
import com.amazonaws.services.simpleworkflow.model.WorkflowType;
import com.github.fzakaria.waterflow.Workflow;
import com.github.fzakaria.waterflow.immutable.ActionId;
import com.github.fzakaria.waterflow.immutable.Control;
import com.github.fzakaria.waterflow.immutable.Details;
import com.github.fzakaria.waterflow.immutable.Domain;
import com.github.fzakaria.waterflow.immutable.Input;
import com.github.fzakaria.waterflow.immutable.Reason;
import com.github.fzakaria.waterflow.immutable.RunId;
import com.github.fzakaria.waterflow.immutable.Tag;
import com.github.fzakaria.waterflow.immutable.TaskListName;
import com.github.fzakaria.waterflow.immutable.WorkflowId;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import org.immutables.builder.Builder;
import org.immutables.value.Value;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static com.github.fzakaria.waterflow.swf.SwfConstants.MAX_NUMBER_TAGS;
import static java.util.stream.Collectors.toList;

@Value.Style(newBuilder = "builder")
public class SwfRequests {

    @Builder.Factory
    public static StartWorkflowExecutionRequest workflowExecutionRequest(
            @Nonnull Workflow<?, ?> workflow,
            @Nonnull Domain domain,
            @Nullable TaskListName taskList,
            WorkflowId workflowId,
            Input input,
            List<Tag> tags,
            @Nullable Duration executionStartToCloseTimeout,
            @Nullable ChildPolicy childPolicy,
            @Nullable Duration taskStartToCloseTimeout) {
        Preconditions.checkArgument(tags.size() < MAX_NUMBER_TAGS,
                "'tags' is longer than supported max length");
        executionStartToCloseTimeout = MoreObjects.firstNonNull(executionStartToCloseTimeout,
                workflow.executionStartToCloseTimeout());
        taskStartToCloseTimeout = MoreObjects.firstNonNull(taskStartToCloseTimeout,
                workflow.taskStartToCloseTimeout());
        childPolicy = MoreObjects.firstNonNull(childPolicy,
                workflow.childPolicy());
        taskList = MoreObjects.firstNonNull(taskList, workflow.taskList());
        return new StartWorkflowExecutionRequest()
                .withWorkflowId(workflowId.value())
                .withDomain(domain.value())
                .withTaskList(new TaskList()
                        .withName(taskList.value()))
                .withWorkflowType(new WorkflowType()
                        .withName(workflow.name().value())
                        .withVersion(workflow.version().value()))
                .withInput(input.value())
                .withTagList(tags.stream().map(Tag::value).collect(toList()))
                .withExecutionStartToCloseTimeout(String.valueOf(executionStartToCloseTimeout.getSeconds()))
                .withTaskStartToCloseTimeout(String.valueOf(taskStartToCloseTimeout.getSeconds()))
                .withChildPolicy(childPolicy.name());
    }

    @Builder.Factory
         public static RegisterWorkflowTypeRequest registerWorkflowTypeRequest(
            @Nonnull Workflow<?, ?> workflow,
            @Nonnull Domain domain) {
        return new RegisterWorkflowTypeRequest()
                .withDomain(domain.value())
                .withDefaultTaskList(new TaskList().withName(workflow.taskList().value()))
                .withName(workflow.name().value())
                .withVersion(workflow.version().value())
                .withDefaultExecutionStartToCloseTimeout(String.valueOf(workflow.executionStartToCloseTimeout().getSeconds()))
                .withDefaultTaskStartToCloseTimeout(String.valueOf(workflow.taskStartToCloseTimeout().getSeconds()))
                .withDefaultChildPolicy(workflow.childPolicy().name())
                .withDescription(workflow.description().value());
    }

    /**
     * Records a WorkflowExecutionTerminated event and forces closure of the workflow execution identified
     * by the given domain, runId, and workflowId. The child policy, registered with the workflow type
     * or specified when starting this execution, is applied to any open child workflow executions
     * of this workflow execution.
     *
     * <b>Note</b>
     * If a runId is not specified, then the WorkflowExecutionTerminated event is recorded in the
     * history of the current open workflow with the matching workflowId in the domain.
     */
    @Builder.Factory
    public static TerminateWorkflowExecutionRequest terminateWorkflowRequest(
            @Nonnull Domain domain,
            @Nonnull WorkflowId workflowId,
            Optional<RunId> runId,
            Optional<Reason> reason,
            Optional<Details> details,
            Optional<ChildPolicy> childPolicy) {
        ChildPolicy cp = childPolicy.orElse(ChildPolicy.TERMINATE);
        return new TerminateWorkflowExecutionRequest()
                .withDomain(domain.value())
                .withWorkflowId(workflowId.value())
                .withRunId(runId.map(RunId::value).orElse(null))
                .withReason(reason.map(Reason::value).orElse(null))
                .withDetails(details.map(Details::value).orElse(null))
                .withChildPolicy(cp);
    }

}

package com.github.fzakaria.waterflow.immutable;

import com.github.fzakaria.waterflow.Workflow;
import com.google.common.base.Preconditions;
import org.immutables.value.Value;

import java.util.concurrent.ThreadLocalRandom;

import static com.github.fzakaria.waterflow.swf.SwfConstants.MAX_ID_LENGTH;
import static com.github.fzakaria.waterflow.swf.SwfUtil.*;
import static java.lang.String.format;

@Value.Immutable
@Wrapped
public abstract class _WorkflowId extends Wrapper<String> {

    @Value.Check
    protected void check() {
        Preconditions.checkState(value().length() < MAX_ID_LENGTH,
                "'workflowId' is longer than supported max length");
    }

    /**
     * Make a unique and valid workflowId.
     * Replaces bad characters and whitespace, appends a random int, and trims to MAX_ID_LENGTH,
     * which also makes it easy for amazon cli use.
     *
     * @param workflowName name of workflow.
     *
     * @return unique workflowId
     */
    public static WorkflowId randomUniqueWorkflowId(Workflow<?,?> workflow) {
        Name name = workflow.name().replaceUnsafeNameChars();
        String randomize = format(".%010d", ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE));
        String nameAsString = trimToMaxLength(name.value(), MAX_ID_LENGTH - randomize.length());
        return WorkflowId.of(assertValidSwfValue(nameAsString + randomize));
    }
}

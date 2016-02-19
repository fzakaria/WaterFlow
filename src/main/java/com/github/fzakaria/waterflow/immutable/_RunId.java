package com.github.fzakaria.waterflow.immutable;

import com.google.common.base.Preconditions;
import org.immutables.value.Value;

import static com.github.fzakaria.waterflow.SwfConstants.MAX_RUN_ID_LENGTH;

@Value.Immutable
@Wrapped
public abstract class _RunId extends Wrapper<String> {

    @Value.Check
    protected void check() {
        Preconditions.checkState(value().length() < MAX_RUN_ID_LENGTH,
                "'runId' is longer than supported max length");
    }
}

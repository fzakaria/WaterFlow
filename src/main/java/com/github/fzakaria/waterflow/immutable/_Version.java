package com.github.fzakaria.waterflow.immutable;

import com.google.common.base.Preconditions;
import org.immutables.value.Value;

import static com.github.fzakaria.waterflow.SwfConstants.MAX_VERSION_LENGTH;

@Value.Immutable
@Wrapped
public abstract class _Version extends Wrapper<String> {

    @Value.Check
    protected void check() {
        Preconditions.checkState(value().length() < MAX_VERSION_LENGTH,
                "'version' is longer than supported max length");
    }
}

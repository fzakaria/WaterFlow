package com.github.fzakaria.waterflow.immutable;

import com.google.common.base.Preconditions;
import org.immutables.value.Value;

import static com.github.fzakaria.waterflow.swf.SwfConstants.MAX_CONTROL_LENGTH;

@Value.Immutable
@Wrapped
abstract class _Control extends Wrapper<String> {

    @Value.Check
    protected void check() {
        Preconditions.checkState(value().length() < MAX_CONTROL_LENGTH,
                "'control' is longer than supported max length");
    }
}

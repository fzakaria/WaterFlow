package com.github.fzakaria.waterflow.immutable;

import com.google.common.base.Preconditions;
import org.immutables.value.Value;

import static com.github.fzakaria.waterflow.swf.SwfConstants.MAX_INPUT_LENGTH;

@Value.Immutable
@Wrapped
abstract class _Input extends Wrapper<String> {

    @Value.Check
    protected void check() {
        Preconditions.checkState(value().length() < MAX_INPUT_LENGTH,
                "'input' is longer than supported max length");
    }
}

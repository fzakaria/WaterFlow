package com.github.fzakaria.waterflow.immutable;

import com.google.common.base.Preconditions;
import org.immutables.value.Value;

import static com.github.fzakaria.waterflow.swf.SwfConstants.MAX_DESCRIPTION_LENGTH;

@Value.Immutable
@Wrapped
public abstract class _Description extends Wrapper<String> {

    @Value.Check
    protected void check() {
        Preconditions.checkState(value().length() < MAX_DESCRIPTION_LENGTH,
                "'taskList' is longer than supported max length");
    }
}

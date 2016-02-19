package com.github.fzakaria.waterflow.immutable;

import com.google.common.base.Preconditions;
import org.immutables.value.Value;

import static com.github.fzakaria.waterflow.SwfConstants.MAX_REASON_LENGTH;

@Value.Immutable
@Wrapped
abstract class _Reason extends Wrapper<String> {

    @Value.Check
    protected void check() {
        Preconditions.checkState(value().length() < MAX_REASON_LENGTH,
                "'reason' is longer than supported max length");
    }
}

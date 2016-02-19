package com.github.fzakaria.waterflow.immutable;

import com.google.common.base.Preconditions;
import org.immutables.value.Value;

import static com.github.fzakaria.waterflow.SwfConstants.MAX_DESCRIPTION_LENGTH;
import static com.github.fzakaria.waterflow.SwfConstants.MAX_NAME_LENGTH;

@Value.Immutable
@Wrapped
public abstract class _Name extends Wrapper<String> {

    @Value.Check
    protected void check() {
        Preconditions.checkState(value().length() < MAX_NAME_LENGTH,
                "'name' is longer than supported max length");
    }

    /**
     * Replace disallowed name characters and whitespace with an underscore.
     *
     * @return string with replacements
     */
    public Name replaceUnsafeNameChars() {
        return Name.of(value().trim()
                .replaceAll("\\s|[^\\w]", "_")
                .replaceAll("arn", "Arn"));
    }
}

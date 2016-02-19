package com.github.fzakaria.waterflow.immutable;

import org.immutables.value.Value;

@Value.Style(
        // Detect names starting with underscore
        typeAbstract = "_*",
        // Generate without any suffix, just raw detected name
        typeImmutable = "*",
        // Make generated it public, leave underscored as package private
        visibility = Value.Style.ImplementationVisibility.PUBLIC)
public @interface Activity {
}

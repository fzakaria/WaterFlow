package com.github.fzakaria.waterflow.immutable;

import org.immutables.value.Value;

@Value.Style(
        // Detect names starting with underscore
        typeAbstract = {"_*"},
        // Generate construction method using all attributes as parameters
        allParameters = true,
        // Changing generated name just for fun
        typeImmutable = "*",
        typeModifiable = "*",
        // We may also disable builder
        defaults = @Value.Immutable(builder = false))
public @interface Tuple {}
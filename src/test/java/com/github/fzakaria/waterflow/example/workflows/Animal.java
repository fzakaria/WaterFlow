package com.github.fzakaria.waterflow.example.workflows;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.github.fzakaria.waterflow.action.ActivityAction;
import com.github.fzakaria.waterflow.immutable.Activity;
import com.github.fzakaria.waterflow.immutable.Tuple;
import com.google.common.base.Preconditions;
import com.google.common.reflect.TypeToken;
import org.immutables.value.Value;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * A simple POJO to demonstrate passing complex types for Activities.
 */
@JsonSerialize(as = ImmutableAnimal.class)
@JsonDeserialize(as = ImmutableAnimal.class)
@Value.Immutable
public abstract class Animal {

    @Activity
    @Value.Immutable
    public static abstract class _AnimalActivityAction extends ActivityAction<Animal> {
        @Override
        public TypeToken<Animal> outputType() {
            return TypeToken.of(Animal.class);
        }
    }

    @JsonSerialize(as = AdamAndEve.class)
    @JsonDeserialize(as = AdamAndEve.class)
    @Value.Immutable
    @Tuple
    public static abstract class _AdamAndEve {
        public abstract Animal adam();
        public abstract Animal eve();
    }

    @Value.Default
    public UUID id() {
        return UUID.randomUUID();
    }

    @Value.Default
    public LocalDateTime dob() {
        return LocalDateTime.now();
    }
    public abstract Optional<Animal> mother();
    public abstract Optional<Animal> father();

    public static Animal mate(Animal mother, Animal father) {
        return null;
    }
}
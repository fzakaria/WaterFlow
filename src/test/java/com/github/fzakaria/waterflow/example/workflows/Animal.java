package com.github.fzakaria.waterflow.example.workflows;

import com.github.fzakaria.waterflow.immutable.Tuple;
import com.google.common.base.Preconditions;
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
@Value.Immutable
public abstract class Animal {

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


    @Value.Check
    protected void check() {
        Preconditions.checkArgument(!mother().equals(father()), "You must have different parents.");
    }

    @Override
    public int hashCode() {
        return id().hashCode();
    }

    @Override
    public boolean equals(Object object) {
        if (object == null) {
            return false;
        }
        if (!(object instanceof Animal)) {
            return false;
        }
        return Objects.equals(id(), ((Animal) object).id());
    }

    public static Animal mate(Animal mother, Animal father) {
        return null;
    }
}
package com.github.fzakaria.waterflow.immutable;

import org.immutables.value.Value;

/**
 @Value.Immutable @Wrapped
 abstract class _LongId extends Wrapper<Long> {}

 @Value.Immutable @Wrapped
 abstract class _PersonName extends Wrapper<String> {}

 @Value.Immutable @Wrapped
 abstract class _VehicleMake extends Wrapper<String> {}

 ...
 // Enjoy your wrapper value types

 LongId id = LongId.of(123L);

 PersonName name = PersonName.of("Vasilij Pupkin");

 VehicleMake make = VehicleMake.of("Honda");
 */
abstract class Wrapper<T> {
    @Value.Parameter
    public abstract T value();

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + value() + ")";
    }
}
package com.github.fzakaria.waterflow.action;

import com.github.fzakaria.waterflow.immutable.Activity;
import com.google.common.reflect.TypeToken;
import org.immutables.value.Value;

@Activity
@Value.Enclosing
public class ActivityActions {

    @Value.Immutable
    public static abstract class _StringActivityAction extends ActivityAction<String>{
        @Override
        public TypeToken<String> outputType() {
            return TypeToken.of(String.class);
        }
    }

    @Value.Immutable
    public static abstract class _IntegerActivityAction extends ActivityAction<Integer>{
        @Override
        public TypeToken<Integer> outputType() {
            return TypeToken.of(Integer.class);
        }
    }

    @Value.Immutable
    public static abstract class _LongActivityAction extends ActivityAction<Integer>{
        @Override
        public TypeToken<Integer> outputType() {
            return TypeToken.of(Integer.class);
        }
    }

    @Value.Immutable
    public static abstract class _ObjectActivityAction extends ActivityAction<Object>{
        @Override
        public TypeToken<Object> outputType() {
            return TypeToken.of(Object.class);
        }
    }

    @Value.Immutable
    public static abstract class _BooleanActivityAction extends ActivityAction<Boolean>{
        @Override
        public TypeToken<Boolean> outputType() {
            return TypeToken.of(Boolean.class);
        }
    }

    @Value.Immutable
    public static abstract class _VoidActivityAction extends ActivityAction<Void>{
        @Override
        public TypeToken<Void> outputType() {
            return TypeToken.of(Void.class);
        }
    }


}

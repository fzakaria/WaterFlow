package com.github.fzakaria.waterflow.immutable;

import com.amazonaws.services.simpleworkflow.model.ActivityType;
import com.github.fzakaria.waterflow.activity.ActivityMethod;
import org.immutables.value.Value;

@Value.Immutable
@Tuple
public abstract class _Key {
    public abstract Name name();

    public abstract Version version();

    public static Key of(ActivityType activityType) {
        return Key.of(Name.of(activityType.getName()), Version.of(activityType.getVersion()));
    }

    public static Key of(ActivityMethod activityMethod) {
        return Key.of(Name.of(activityMethod.name()), Version.of(activityMethod.version()));
    }

}

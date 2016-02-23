package com.github.fzakaria.waterflow.immutable;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.github.fzakaria.waterflow.event.Event;
import org.immutables.value.Value;

import java.util.List;

@Value.Modifiable
@Tuple
public abstract class _DecisionContext {

    public abstract List<Event> events();

    public abstract List<Decision> decisions();
}

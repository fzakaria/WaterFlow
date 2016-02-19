package com.github.fzakaria.waterflow.poller;


import com.github.fzakaria.waterflow.Workflow;
import com.github.fzakaria.waterflow.immutable.Name;
import org.immutables.value.Value;

import java.util.List;
import java.util.stream.IntStream;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

/**
 * Launch a pool of {@link DecisionPoller} and register the example workflows on each poller instance.
 *
 **/
@Value.Immutable
public abstract class DecisionPollerPool extends BasePollerPool<DecisionPoller> {

    public abstract List<Workflow<?,?>> workflows();

    @Override
    public Name name() {
        return Name.of("DECIDER");
    }

    @Override
    protected List<DecisionPoller> constructPollers(int size) {
        return IntStream.range(0, size).mapToObj(i ->
                        ImmutableDecisionPoller.builder().name(Name.of(format("%s-%s", name().value(),i)))
                                .domain(domain()).swf(swf()).dataConverter(dataConverter())
                                .taskList(taskList()).workflows(workflows()).build()
        ).collect(toList());
    }

}
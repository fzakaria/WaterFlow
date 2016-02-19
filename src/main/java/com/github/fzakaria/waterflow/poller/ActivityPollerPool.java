package com.github.fzakaria.waterflow.poller;

import com.github.fzakaria.waterflow.Activities;
import com.github.fzakaria.waterflow.immutable.Name;
import org.immutables.value.Value;

import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.stream.IntStream;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

/**
 * A helper class that facilitates running multiple {@link ActivityPoller} for a given {@link ScheduledThreadPoolExecutor}
 * A helpful {@link ThreadFactory} is set which names the threads with 'ACTIVITY'
 *
 */
@Value.Immutable
public abstract class ActivityPollerPool extends BasePollerPool<ActivityPoller> {

    public abstract List<Activities> activities();

    @Override
    public Name name() {
        return Name.of("ACTIVITY");
    }

    @Override
    protected List<ActivityPoller> constructPollers(int size) {
        return IntStream.range(0, size).mapToObj(i ->
                        ImmutableActivityPoller.builder().name(Name.of(format("%s-%s", name().value(),i)))
                                .domain(domain()).swf(swf()).dataConverter(dataConverter())
                                .taskList(taskList()).activities(activities()).build()
        ).collect(toList());
    }

}

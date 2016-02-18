package com.github.fzakaria.waterflow.poller;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.github.fzakaria.waterflow.Activities;
import com.github.fzakaria.waterflow.converter.DataConverter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.lang.String.format;

/**
 * A helper class that facilitates running multiple {@link ActivityPoller} for a given {@link ScheduledThreadPoolExecutor}
 * A helpful {@lin ThreadFactory} is set which names the threads with 'ACTIVITY'
 *
 */
@Value
@Slf4j
public class ActivityPollerPool {

    private final ScheduledThreadPoolExecutor service;
    private final String taskList;
    private final String domain;
    private final AmazonSimpleWorkflow swf;
    private final DataConverter dataConverter;
    private final Collection<Activities> activities = Lists.newArrayList();

    /***
     * Create a {@link ActivityPollerPool}
     * @param service The {@link java.util.concurrent.ThreadPoolExecutor} to be using
     * @param taskList The taskList that these pollers should be asking for work
     * @param domain The domain that these pollers should be asking for work
     * @param swf The swf client
     * @param dataConverter The serializer/deserializer
     */
    public ActivityPollerPool(ScheduledThreadPoolExecutor service, String taskList, String domain, AmazonSimpleWorkflow swf,
                              DataConverter dataConverter) {
        this.service = service;
        this.taskList = taskList;
        this.domain = domain;
        this.swf = swf;
        this.dataConverter = dataConverter;
        this.service.setThreadFactory(executorThreadFactory());
    }

    /**
     * Create a {@link ActivityPollerPool}
     * @param taskList The taskList that these pollers should be asking for work
     * @param domain The taskList that these pollers should be asking for work
     * @param numOfWorkers The numbe rof workers for {@link ScheduledThreadPoolExecutor}
     * @param swf The swf client
     * @param dataConverter The serializer/deserializer
     */
    public ActivityPollerPool(String taskList, String domain, int numOfWorkers, AmazonSimpleWorkflow swf, DataConverter dataConverter) {
        this(new ScheduledThreadPoolExecutor(numOfWorkers), taskList, domain, swf, dataConverter);
    }

    /**
     * Register activities that will be polled for
     * @param activities
     */
    public void activities(Activities... activities) {
        this.activities.addAll(Arrays.asList(activities));
    }

    /**
     * Start the pollers. The number of workers scheduled are equal to exactly
     * the {@link ScheduledThreadPoolExecutor#getCorePoolSize()}
     */
    public void start() {
        int numOfWorkers = service.getCorePoolSize();
        List<ActivityPoller> pollers =
                IntStream.range(0, numOfWorkers).mapToObj(i -> {
                            ActivityPoller poller = new ActivityPoller(format("ACTIVITY-%s", i), taskList,domain, swf, dataConverter);
                            poller.addActivities(Iterables.toArray(activities, Activities.class));
                            return poller;
                        }
                ).collect(Collectors.toList());
        pollers.stream().findAny().ifPresent(BasePoller::registerDomain);
        pollers.stream().findAny().ifPresent(ActivityPoller::registerSwfActivities);
        pollers.stream().forEach( p ->  {
            log.info(format("start: %s domain=%s taskList=%s", p.getId(), domain, taskList));
            service.scheduleWithFixedDelay(p, 1, 1, TimeUnit.SECONDS);
        });
    }

    /**
     * Shutdown the {@link ScheduledThreadPoolExecutor}  and the {@link AmazonSimpleWorkflow} client
     */
    public void stop() {
        this.service.shutdownNow();
    }

    private ThreadFactory executorThreadFactory() {
        ThreadFactoryBuilder threadFactoryBuilder
                = new ThreadFactoryBuilder().setNameFormat(format("%s-%s-%%d", "ACTIVITY", taskList));
        return threadFactoryBuilder.build();
    }
}

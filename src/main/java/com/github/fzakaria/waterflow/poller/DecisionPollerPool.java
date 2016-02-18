package com.github.fzakaria.waterflow.poller;


import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.github.fzakaria.waterflow.Workflow;
import com.github.fzakaria.waterflow.converter.DataConverter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
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
 * Launch a pool of {@link DecisionPoller} and register the example workflows on each poller instance.
 *
 **/
@Value
@Slf4j
public class DecisionPollerPool {

    private final ScheduledThreadPoolExecutor service;
    private final String taskList;
    private final String domain;
    private final AmazonSimpleWorkflow swf;
    private final DataConverter dataConverter;
    private final Collection<Workflow> workflows = Lists.newArrayList();

    public DecisionPollerPool(ScheduledThreadPoolExecutor service, String taskList, String domain, AmazonSimpleWorkflow swf, DataConverter dataConverter) {
        this.service = service;
        this.taskList = taskList;
        this.domain = domain;
        this.swf = swf;
        this.dataConverter = dataConverter;
        this.service.setThreadFactory(getExecutorThreadFactory());
    }

    public DecisionPollerPool(String taskList, String domain, int numOfWorkers, AmazonSimpleWorkflow swf, DataConverter dataConverter) {
        this(new ScheduledThreadPoolExecutor(numOfWorkers), taskList, domain, swf, dataConverter);
    }

    /**
     * Add the {@link Workflow} that the decisions know how to handle
     * @param workflows The list of known workflows
     */
    public void workflows(Workflow... workflows) {
        this.workflows.addAll(Arrays.asList(workflows));
    }

    /**
     * Start the pollers. The number of pollers is dictated by the
     * {@link ScheduledThreadPoolExecutor#getCorePoolSize()}
     */
    public void start() {
        int numOfWorkers = service.getCorePoolSize();

        List<DecisionPoller> pollers =
                IntStream.range(0, numOfWorkers).mapToObj(i -> {
                            String pollerId = format("DECIDER-%d", i);
                            DecisionPoller poller = new DecisionPoller(pollerId, domain, taskList,swf, dataConverter);
                            poller.addWorkflows(Iterables.toArray(workflows, Workflow.class));
                            return poller;
                        }
                ).collect(Collectors.toList());
        pollers.stream().findAny().ifPresent( p -> p.registerDomain() );
        pollers.stream().findAny().ifPresent( p -> p.registerSwfWorkflows() );
        pollers.stream().forEach( p ->  {
            log.info(format("start: %s domain=%s taskList=%s", p.getId(), domain, taskList));
            service.scheduleWithFixedDelay(p, 1, 1, TimeUnit.SECONDS);
        });
    }

    /**
     * Shutdown the {@link ScheduledThreadPoolExecutor}  and the {@link AmazonSimpleWorkflow} client
     */
    public void stop() {
        service.shutdownNow();
        swf.shutdown();
    }

    private ThreadFactory getExecutorThreadFactory() {
        ThreadFactoryBuilder threadFactoryBuilder
                = new ThreadFactoryBuilder().setNameFormat(format("%s-%s-%%d", "DECIDER", taskList));
        return threadFactoryBuilder.build();
    }
}
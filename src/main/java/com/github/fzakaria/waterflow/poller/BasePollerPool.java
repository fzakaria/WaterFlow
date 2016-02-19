package com.github.fzakaria.waterflow.poller;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.github.fzakaria.waterflow.converter.DataConverter;
import com.github.fzakaria.waterflow.immutable.Domain;
import com.github.fzakaria.waterflow.immutable.Name;
import com.github.fzakaria.waterflow.immutable.TaskListName;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;


public abstract class BasePollerPool<PollerType extends BasePoller> {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    public abstract ScheduledThreadPoolExecutor service();
    public abstract TaskListName taskList();
    public abstract Domain domain();
    public abstract AmazonSimpleWorkflow swf();
    public abstract DataConverter dataConverter();
    public abstract Name name();

    public void start() {
        //set the threadfactory for pretty names
        service().setThreadFactory(executorThreadFactory());

        int numOfWorkers = service().getCorePoolSize();
        List<PollerType> pollers =  constructPollers(numOfWorkers);
        pollers.stream().findAny().ifPresent(BasePoller::registerDomain);
        pollers.stream().findAny().ifPresent(BasePoller::register);
        pollers.stream().forEach( p ->  {
            log.info(format("start: %s domain=%s taskList=%s", p.name(), p.domain(), p.taskList()));
            service().scheduleWithFixedDelay(p, 1, 1, TimeUnit.SECONDS);
        });
    }

    /**
     * Shutdown the {@link ScheduledThreadPoolExecutor}  and the {@link AmazonSimpleWorkflow} client
     */
    public void stop() {
        service().shutdownNow();
    }

    protected abstract List<PollerType> constructPollers(int size);

    protected ThreadFactory executorThreadFactory() {
        ThreadFactoryBuilder threadFactoryBuilder
                = new ThreadFactoryBuilder().setNameFormat(format("%s-%s-%%d", name().value(), taskList().value()));
        return threadFactoryBuilder.build();
    }

}

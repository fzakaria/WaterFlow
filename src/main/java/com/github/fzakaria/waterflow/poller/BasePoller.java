package com.github.fzakaria.waterflow.poller;


import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.model.DomainAlreadyExistsException;
import com.amazonaws.services.simpleworkflow.model.RegisterDomainRequest;
import com.github.fzakaria.waterflow.immutable.Domain;
import com.github.fzakaria.waterflow.immutable.Name;
import com.github.fzakaria.waterflow.immutable.TaskListName;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Period;

import static com.github.fzakaria.waterflow.SwfConstants.*;

/**
 * Base class for Activity and Decision pollers.
 *
 */
public abstract class BasePoller implements Runnable {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    public abstract Name name();
    public abstract TaskListName taskList();
    public abstract Domain domain();
    public abstract AmazonSimpleWorkflow swf();
    @Value.Default
    public Period domainRetention() {
        return MAX_DOMAIN_RETENTION;
    }

    /**
     * {@link Runnable#run} implementation calls {@link #poll()} once,
     * allows for scheduling multiple poller instances in an external thread pool.
     *
     * @see #poll
     */
    public void run() {
        log.trace("Beginning poll execution.");
        try {
            poll();
        } catch (Throwable t) {
            log.error("Unexpected throwable during poll.", t);
        }
    }

    /**
     * Subclass implements to perform the SWF polling work.
     *
     * @see #run
     */
    protected abstract void poll();

    /**
     * Register domain if it does not exist already
     * {@link DomainAlreadyExistsException} are ignored making this method idempotent.
     *
     */
    public void registerDomain() {
        log.trace("Attempting to register {} domain for {}", domain(), domainRetention());
        try {
            swf().registerDomain(new RegisterDomainRequest().withName(domain().value())
                    .withWorkflowExecutionRetentionPeriodInDays(String.valueOf(domainRetention().getDays())));
        }
        catch (DomainAlreadyExistsException e) {
            log.trace("SwfDomain {} is already registered",domain());
        }
    }

    /**
     * Register the specific types this poller
     * is polling for
     */
    public abstract void register();


}
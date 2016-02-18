package com.github.fzakaria.waterflow.poller;


import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.model.DomainAlreadyExistsException;
import com.amazonaws.services.simpleworkflow.model.RegisterDomainRequest;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;

import static com.github.fzakaria.waterflow.SwfConstants.*;
/**
 * Base class for Activity and Decision pollers.
 *
 * @author Farid Zakaria
 */
@Value
@Slf4j
@EqualsAndHashCode(of="id")
public @NonFinal abstract class BasePoller implements Runnable {
    protected final String id;
    protected final String taskList;
    protected final String domain;
    protected final AmazonSimpleWorkflow swf;

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
        log.trace("Attempting to register {} domain for {}", domain, MAX_DOMAIN_RETENTION);
        try {
            swf.registerDomain(new RegisterDomainRequest().withName(domain)
                    .withWorkflowExecutionRetentionPeriodInDays(String.valueOf(MAX_DOMAIN_RETENTION.getDays())));
        }
        catch (DomainAlreadyExistsException e) {
            log.trace("SwfDomain {} is already registered",domain);
        }
    }


}
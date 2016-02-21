package com.github.fzakaria.waterflow.example;

import com.github.fzakaria.waterflow.Workflow;
import com.github.fzakaria.waterflow.example.workflows.ImmutableAdvancedInputWorkflow;
import com.github.fzakaria.waterflow.example.workflows.ImmutableExampleActivities;
import com.github.fzakaria.waterflow.example.workflows.ImmutableHeartbeatWorkflow;
import com.github.fzakaria.waterflow.example.workflows.ImmutableSimpleMarkerWorkflow;
import com.github.fzakaria.waterflow.example.workflows.ImmutableSimpleWorkflow;
import com.github.fzakaria.waterflow.example.workflows.ImmutableThrowingWorkflow;
import com.github.fzakaria.waterflow.example.workflows.ImmutableTimerWorkflow;
import com.github.fzakaria.waterflow.poller.ActivityPollerPool;
import com.github.fzakaria.waterflow.poller.DecisionPollerPool;
import com.github.fzakaria.waterflow.poller.ImmutableActivityPollerPool;
import com.github.fzakaria.waterflow.poller.ImmutableDecisionPollerPool;
import com.google.common.collect.Lists;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;

@Value.Immutable
public abstract class ActivityDecisionPollerPool {

    protected static final Logger log = LoggerFactory.getLogger(ActivityDecisionPollerPool.class);

    public abstract Config config();

    @Value.Derived
    public ActivityPollerPool activityPollerPool() {
        return ImmutableActivityPollerPool.builder().domain(config().domain())
                .taskList(config().taskList())
                .service(new ScheduledThreadPoolExecutor(config().activityPoolSize()))
                .swf(config().swf())
                .dataConverter(config().dataConverter())
                .addActivities(ImmutableExampleActivities.builder().build()).build();
    }

    @Value.Derived
    public DecisionPollerPool decisionPollerPool() {
        List<Workflow<?,?>> workflows = Lists.newArrayList(
                ImmutableSimpleWorkflow.builder().dataConverter(config().dataConverter()).build(),
                ImmutableAdvancedInputWorkflow.builder().dataConverter(config().dataConverter()).build(),
                ImmutableThrowingWorkflow.builder().dataConverter(config().dataConverter()).build(),
                ImmutableSimpleMarkerWorkflow.builder().dataConverter(config().dataConverter()).build(),
                ImmutableTimerWorkflow.builder().dataConverter(config().dataConverter()).build(),
                ImmutableHeartbeatWorkflow.builder().dataConverter(config().dataConverter()).build()
        );
        return ImmutableDecisionPollerPool.builder().domain(config().domain())
                .taskList(config().taskList())
                .service(new ScheduledThreadPoolExecutor(config().activityPoolSize()))
                .swf(config().swf())
                .dataConverter(config().dataConverter())
                .workflows(workflows).build();
    }

    public void start() {
        activityPollerPool().start();
        decisionPollerPool().start();
    }

    public void stop() {
        activityPollerPool().stop();
        decisionPollerPool().stop();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        Config config = ImmutableConfig.of();
        ActivityDecisionPollerPool activityAndDecisionPollerPool =
                ImmutableActivityDecisionPollerPool.builder().config(config).build();

        activityAndDecisionPollerPool.start();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                log.info("Shutting down pool and exiting.");
                activityAndDecisionPollerPool.stop();
            }
        });
        log.info("activity pollers started:");
    }
}

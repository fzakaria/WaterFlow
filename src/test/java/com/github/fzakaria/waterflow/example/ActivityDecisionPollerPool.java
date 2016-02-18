package com.github.fzakaria.waterflow.example;

import com.github.fzakaria.waterflow.example.workflows.ExampleActivities;
import com.github.fzakaria.waterflow.example.workflows.AdvancedInputWorkflow;
import com.github.fzakaria.waterflow.example.workflows.SimpleWorkflow;
import com.github.fzakaria.waterflow.poller.ActivityPollerPool;
import com.github.fzakaria.waterflow.poller.DecisionPollerPool;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
@Value
public class ActivityDecisionPollerPool {

    private final ActivityPollerPool activityPollerPool;
    private final DecisionPollerPool decisionPollerPool;

    public ActivityDecisionPollerPool(Config config) {

        activityPollerPool = new ActivityPollerPool(config.getTaskList(),
                config.getDomain(),
                config.getActivityPoolSize(),
                config.getAmazonSimpleWorkflow(),
                config.getDataConverter());

        activityPollerPool.activities(new ExampleActivities());

        decisionPollerPool = new DecisionPollerPool(config.getTaskList(),
                config.getDomain(),
                config.getDecisionPoolSize(),
                config.getAmazonSimpleWorkflow(),
                config.getDataConverter());

        decisionPollerPool.workflows(new SimpleWorkflow(), new AdvancedInputWorkflow());
    }

    public void start() {
        activityPollerPool.start();
        decisionPollerPool.start();
    }

    public void stop() {
        activityPollerPool.stop();
        decisionPollerPool.stop();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        Config config = new Config();
        ActivityDecisionPollerPool activityAndDecisionPollerPool = new ActivityDecisionPollerPool(config);

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

package com.github.fzakaria.waterflow.example.workflows;

import com.github.fzakaria.waterflow.Activities;
import com.github.fzakaria.waterflow.activity.ActivityMethod;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

import static java.lang.String.format;

/**
 * Sample ExampleActivities showing a wide range of features.
 */
@Value.Immutable
public abstract  class ExampleActivities extends Activities {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @ActivityMethod(name = "Sleep", version = "1.0")
    public Void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            throw new IllegalStateException("Interrupted", e);
        }
        return null;
    }

    /**
     * There is support for variadic methods however the way in which you supply the input to the
     * Activity is important. All variadic input needs to be given as an object array.
     * This is to help distinguish it.
     * <pre>step1.input(new Object[] {i, 1})</pre>
     * @return
     */
    @ActivityMethod(name = "Addition", version = "1.0")
    public Integer addition(Integer lhs, Integer rhs) {
        return lhs + rhs;
    }

    @ActivityMethod(name = "Division", version = "1.0")
    public Integer division(Integer dividend, Integer divisor) {
        if (divisor == 0) {
            throw new ArithmeticException("Cannot divide by 0.");
        }
        return dividend/divisor;
    }

    @ActivityMethod(name = "Echo", version = "1.0")
    public void echo(Object[] arguments) {
        for(Object arg : arguments) {
            log.info("Received input: {}", arg);
        }
    }

    @ActivityMethod(name = "Mate", version = "1.0")
    public Animal mate(Animal male, Animal female) {
        return Animal.mate(male, female);
    }

    /**
     * A simple method that only executes/completes happily if the recordHeartbeat function
     * occurs as expected. The heartbeat timeout is to "5" however there is a sleep of "10" seconds.
     */
    @ActivityMethod(name = "Heartbeat", version = "1.0", heartbeatTimeout = "5")
    public Void heartbeat() throws InterruptedException {
        final LongAdder adder = new LongAdder();
        final ScheduledExecutorService service = Executors.newScheduledThreadPool(1);
        service.scheduleAtFixedRate(() -> {
            adder.increment();
            recordHeartbeat(format("This is the %s heartbeat", adder.intValue()));
        }, 0, 1, TimeUnit.SECONDS);
        Thread.sleep(Duration.ofSeconds(10).toMillis());
        service.shutdownNow();
        return null;
    }


}

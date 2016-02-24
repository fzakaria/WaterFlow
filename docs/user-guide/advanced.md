
## Throwables

Things don't always go as planned and neither do your `ActivityTask`. Through the help of the `DataConverter`, `Throwables` are marshalled from the Activity to the decider.  That means you can write decider logic that approriately accounts for exceptions that may occur during the execution of an ActivityTask.

A common scenario may be that you would want to run some cleanup actions if an exception is thrown.

## Heartbeat

During Activity registration (occurs automatically at the start of the `ActivityPollerPool`) a `taskHeartbeatTimeout` is set. The `taskHeartbeatTimeout` is defined as the longest time an `AcitivityTask` may perform work before either responding with the result or heartbeat. For activities that run extra long (since a Workflow is allowed to run for up to 1 year!) you'll need to periodically send heartbeats during the Acitvity.

All ActivityMethods must be present in a class that subclasses from `com.github.fzakaria.waterflow.Activities` which includes the *protected* method
`protected void recordHeartbeat(String details)`

Higher level abstractions may find it necessary to wrap the heartbeat functionality in a [Watch Dog Thread](https://en.wikipedia.org/wiki/Watchdog_timer).

## Retries

It would be a PITA if you had to correctly handdle *transient* exceptions thrown in your ActivityTask - such as service intermittent outage or consistency check or even IOExceptions during HTTP requests.

Since the concept of retrying intermittent failures is so common, `WaterFlow` provides a simple way to register a `RetryStrategy` to the `ActivityAction`.
If any `Throwable` escapes during the invocation of an `ActivityTask`, the `RetryStrategy` is used to determine how long a `Timer` should be created to delay until the next attempt.

Several strategies are included in the framework:

1. TimeLimitRetryStrategy
2. MaxAttemptsRetryStrategy
3. ExponentialBackoffStrategy
4. NoRetryStrategy
5. MaxLimitRetryStrategy
6. FixedDelayRetryStrategy

```
final IntegerActivityAction step1 = IntegerActivityAction.builder()
            .actionId(ActionId.of("step1")).retryStrategy(new FixedDelayRetryStrategy(Duration.ofSeconds(3)))
            .name(Name.of("Hello World")).version(Version.of("1.0")).workflow(this).build();
```

## DataConverters 

The `WaterFlow` framework marshals data across activities & deciders by using a `DataConverter`. By default, the framework offers a data converter that is based on the Jackson JSON processor - similar to the *AWS Flow Framework*.

If the default converter isn't sufficient for your application, you can implement a custom data converter. A good example is offering a `DataConverter` that might encrypt/decrypt data sent over the wire or marshalling data through `S3` to overcome the size limitation of the fields put in palce by SWF
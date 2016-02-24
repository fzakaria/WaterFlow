# WaterFlow

WaterFlow is a *non-magical* - relatively small framework for use with [Amazon SWF](https://aws.amazon.com/swf/).
Browse the [online documentation](http://fzakaria.github.io/WaterFlow/ )

# Philosophy

The framework is deeply routed in the original implementation of [SWiFt](https://bitbucket.org/clarioanalytics/services-swift)
which is a great example of achieving a very usable and simple SWF framework.

WaterFlow aims to add new features that are common in the [AWS Flow Framework](https://aws.amazon.com/swf/details/flow/)
without the need for [AspectJ](https://eclipse.org/aspectj/) and the *magical* annotation processing that occurs.

Here is a short list of philosophy decisions that differentiate this framework from [SWiFt](https://bitbucket.org/clarioanalytics/services-swift)
and [Glisten](https://github.com/Netflix/glisten/)

1. JDK8 target - heavy use of new JDK8 features such as Optional and Streams make for simpler code
2. Allow arbitrary types to be passed as input or sent as output for a given Activity through the use of a `DataConverter`
3. Deciders are written through asynchronous interfaces - [CompletionStage](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletionStage.html) - without any magic!
4. Heavy **Heavy** use of of the [Immutables](https://immutables.github.io/) library


# Requirements

1. Maven 2 or 3 installation
2. Java 1.8+
3. Active Amazon Web Services account and credentials
4. Access to Amazon SWF with a domain set up
5. Understanding of Amazon Simple Workflow concepts and the Amazon AWS SDK for Java

```xml
<dependency>
    <groupId>com.github.fzakaria</groupId>
    <artifactId>WaterFlow</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

# Samples

Several examples have been included within the [com.github.fzakaria.waterflow.example](src/test/java/com/github/fzakaria/waterflow/example/) package that demonstrate the full
range of features offered by the framework.

You can use them as a learning example or even run them yourself.
Included are some *integration tests* that verify that the workflows perform their expected operations.
Take a look at the [ExamplesIntegrationTest.java](src/test/java/com/github/fzakaria/waterflow/example/ExamplesIntegrationTest.java) file.

# Show me some code !
> Talk is cheap, show me some code

```java
@Value.Immutable
public abstract  class ExampleActivities extends Activities {
    @ActivityMethod(name = "Addition", version = "1.0")
    public Integer addition(Integer lhs, Integer rhs) {
            return lhs + rhs;
    }
}
```

```java
@Value.Immutable
public abstract class SimpleWorkflow extends Workflow<Integer, Integer> {

    final IntegerActivityAction step1 = IntegerActivityAction.builder().actionId(ActionId.of("step1"))
            .name(Name.of("Addition")).version(Version.of("1.0")).workflow(this).build();
    @Override
    public CompletionStage<Integer> decide(DecisionContext decisionContext) {
        // Set a breakpoint below to watch the decisions list to see what gets added on each call to Workflow.decide()
        CompletionStage<Integer> input = workflowInput(decisionContext.events());
        return input
                .thenCompose(i -> step1.withInput(i, 1).decide(decisionContext))
                .thenCompose(step1i -> step2.withInput(step1i, 100).decide(decisionContext));

    }
    
}
```

# Features

1. Write deciders in fluent Asynchronous interfaces using [CompletionStage](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletionStage.html)
2. Many Actions supported in the deciders
  1. ActivityAction - Bread and butter of creating activities with generic input/out using `DataConverter`
  2. RecordMarkerAction - Record arbitrary diagnostic information during the decider to help debugging
  3. TimerAction - Create timers in the decider to wait for a specific time interval before proceeding
  4. WaitSignalAction - Wait on an external stimuli (could be human intervention) before proceeding in the workflow logic
3. Extra long ActivityActions can emit a heartbeat to make sure they continue beyond acceptable time limit
4. Automatically retry failed ActivityActions. Several retry strategies (including exponential backoff) provided.

# TODO

2. Add 'StartChildWorkflow' Action
3. Add Send 'Signal' Action
4. Add Spring/Guice as 'Optional' dependencies. Introduce appropriate new workflow scopes and sample configuration setup.
5. Fixup some overuse of the Immutables library
6. Add metrics

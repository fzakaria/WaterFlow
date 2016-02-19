# WaterFlow

WaterFlow is a *non-magical* - relatively small framework for use with [Amazon SWF](https://aws.amazon.com/swf/).

# Philosophy

The framework is deeply routed in the original implementation of [SWiFt](https://bitbucket.org/clarioanalytics/services-swift)
which is a great example of achieving a very usable and simple SWF framework.

WaterFlow aims to add new features that are common in the [AWS Flow Framework](https://aws.amazon.com/swf/details/flow/)
without the need for [AspectJ](https://eclipse.org/aspectj/) and the *magical* annotation processing that occurs.

Here is a short list of philosphy decisions that differentiate this framework from [SWiFt](https://bitbucket.org/clarioanalytics/services-swift)
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

Several examples have been included within the `com.github.fzakaria.waterflow.example` package that demonstrate the full
range of features offered by the framework.

You can use them as a learning example or even run them yourself.
Included are some *integration tests* that verify that the workflows perform their expected operations.
Take a look at the `ExamplesIntegrationTest.java` file.

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
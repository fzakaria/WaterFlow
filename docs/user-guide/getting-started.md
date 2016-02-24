# Getting Started 

The following tutorial will walk you through a very brief introduction to installing `WaterFlow` and writing your first
`HelloWorldWorkflow`.

For more advanced workflows and additional information consult the user-guide.

## Requirements

1. Java 1.8+
2. Active `Amazon Web Services` account
	* AWS credentials must have full access to the SWF service
3. Understanding of Amazon Simple Workflow concepts and the Amazon AWS SDK for Java

## Installation

```xml
<dependency>
    <groupId>com.github.fzakaria</groupId>
    <artifactId>WaterFlow</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

## HelloWorld*WorkFlow*

The following is a short tutorial on building a Maven application using `WaterFlow`. It is a very high level overview and additional information can be found in accompanying sections of the user-guide.

You can find the code listed below on [GitHub](https://github.com/fzakaria/WaterFlow)
### Setup

We will assume a [maven](https://maven.apache.org) setup, however the only difference is transitioning the `pom.xml` to the build tool of your choice.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>FILL ME IN/groupId>
    <artifactId>FILL ME IN/artifactId>
    <version>1.0-SNAPSHOT</version>

    <properties>
        <maven.compiler.source>1.8</maven.compiler.source>
        <maven.compiler.target>1.8</maven.compiler.target>
        <immutables.version>2.1.11</immutables.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.immutables</groupId>
            <artifactId>value</artifactId>
            <version>${immutables.version}</version>
            <scope>provided</scope>
        </dependency>

        <dependency>
            <groupId>com.github.fzakaria</groupId>
            <artifactId>WaterFlow</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>

        <dependency>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-classic</artifactId>
            <version>1.1.5</version>
            <scope>runtime</scope>
        </dependency>
    </dependencies>
</project>

</project>
```

### Activities

The bread and butter of actions in SWF are the [ActivityTasks](http://docs.aws.amazon.com/amazonswf/latest/apireference/API_ActivityTask.html), which represent a unit of asyncrhonous work. Activities can exist in any class that subclasses `Activities` and annotated with `ActivityMethod`

```java
public class ExampleActivities extends Activities {
    
    @ActivityMethod(name = "Hello World", version = "1.0")
    public String helloWorld(String name) {
    	if (name == null) {
    		throw new IllegalArgumentException("You must provide a non null name!");
    	}
        return String.format("Hello World %s", name);
    }

}
```

In the example above, we've created a very simple `ActivityTask` which given an input string, appends it to a *Hello World* message. The example above is working solely with input/output of **String** however you are not limited to that! We will see in the following documentation that thanks to the `DataConverter` we can pass arbitrary complex types to/from our Activities.

### Workflow

The workflow class in the framework represents the `Decider`. Through the interface `public CompletionStage decide(DecisionContext decisionContext)` the Workflow can orchestrate the future actions that need to occur for the workflow given the previous history.

The example below is a simple example of implementing the Workflow interface. Each workflow has an associated name and version which is important when decided which instance the decider needs to find. As you can see in the `decide` method, orchestrating future tasks is as simple as using the JDK8 interface for `CompletionStage`.

```java
@Value.Immutable
public class HelloWorldWorkflow extends Workflow<String,String> {

    @Override
    public Name name() {
        return Name.of("Hello World");
    }

    @Override
    public Version version() {
        return Version.of("1.0");
    }

    @Override
    public TypeToken<String> inputType() {
        return TypeToken.of(String.class);
    }

    @Override
    public TypeToken<String> outputType() {
        return TypeToken.of(String.class);
    }

    @Override
    public DataConverter dataConverter() {
        return ImmutableJacksonDataConverter.builder().build();
    }

    final StringActivityAction step1 = StringActivityAction.builder().actionId(ActionId.of("step1"))
            .name(Name.of("Hello World")).version(Version.of("1.0")).workflow(this).build();

    @Override
    public CompletionStage decide(DecisionContext decisionContext) {
        CompletionStage<String> input = workflowInput(decisionContext.events());

        return input.thenCompose(i -> step1.withInput(i).decide(decisionContext));

    }

    //Run me to submit this workflow!
    public static void main(String[] args) throws IOException, InterruptedException {
        Config config = new Config();
        HelloWorldWorkflow workflow = ImmutableHelloWorldWorkflow.builder()
                .description(Description.of("Starting my first workflow!")).build();
        config.submit(workflow, "Jane Doe");
    }
}
```

We've provided a helpful `main` method that you can launch that will submit a new instance of this workflow!

### Actors - Deciders and Activity Workers

You get to decide the composition of the actors. You can scale the deciders and acivity workers seperately and arrange how each one is configured when polling a particular `TaskList` or `Domain`. For the purposes of this example, we demonstrate a basic setup that initiates both `Activity Workers` and `Deciders` as separate threads on the same JVM.

```java
public class ActivityDecisionPollerPool {

    protected static final Logger log = LoggerFactory.getLogger(ActivityDecisionPollerPool.class);

    private final Config config = new Config();

    private final  ActivityPollerPool activityPollerPool =
            ImmutableActivityPollerPool.builder().domain(config.domain)
                .taskList(config.taskListName)
                .service(new ScheduledThreadPoolExecutor(config.numberOfWorkers))
                .swf(config.swf)
                .dataConverter(config.dataConverter)
                .addActivities(new HelloWorldActivities()).build();

    private final  DecisionPollerPool decisionPollerPool =
            ImmutableDecisionPollerPool.builder().domain(config.domain)
                    .taskList(config.taskListName)
                    .service(new ScheduledThreadPoolExecutor(config.numberOfWorkers))
                    .swf(config.swf)
                    .dataConverter(config.dataConverter)
                    .workflows(Lists.newArrayList(new HelloWorldWorkflow())).build();


    public void start() {
        activityPollerPool.start();
        decisionPollerPool.start();
    }

    public void stop() {
        activityPollerPool.stop();
        decisionPollerPool.stop();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        ActivityDecisionPollerPool activityAndDecisionPollerPool =
                new ActivityDecisionPollerPool();

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
```
For the purposes of this demonstrates, the configuration is hard configured in a POJO. However not much is needed to change the `Config` class to something that is wired by your favourite Dependency Injection framework (i.e. Guice or Spring)

```java

public class Config {

    public final Domain domain = Domain.of("swift");

    public final TaskListName taskListName = SwfConstants.DEFAULT_TASK_LIST;

    public final Integer numberOfWorkers = 2;

    public final DataConverter dataConverter = ImmutableJacksonDataConverter.builder().build();

    //SWF holds the connection for 60 seconds to see if a decision is available
    final Duration DEFAULT_CONNECTION_TIMEOUT = Duration.ofSeconds(60);
    final Duration DEFAULT_SOCKET_TIMEOUT = DEFAULT_CONNECTION_TIMEOUT.plusSeconds(10);
    public final AmazonSimpleWorkflow swf =  new AmazonSimpleWorkflowClient(new DefaultAWSCredentialsProviderChain(),
            new ClientConfiguration().withConnectionTimeout((int) DEFAULT_CONNECTION_TIMEOUT.toMillis())
                    .withSocketTimeout((int) DEFAULT_SOCKET_TIMEOUT.toMillis()));

    public WorkflowExecution submit(Workflow workflow, WorkflowId workflowId, Optional<Object> input) {
        Optional<Input> inputOptional = input.map( i -> dataConverter.toData(i)).map(Input::of);

        StartWorkflowExecutionRequest request =
                WorkflowExecutionRequestBuilder.builder().domain(domain)
                        .workflow(workflow).input(inputOptional)
                        .taskList(taskListName).workflowId(workflowId).build();

        Run run = swf.startWorkflowExecution(request);
        return new WorkflowExecution().withWorkflowId(workflowId.value()).withRunId(run.getRunId());
    }

    public <I, O> WorkflowExecution submit(Workflow<I,O> workflow, I input) {
        WorkflowId workflowId = WorkflowId.randomUniqueWorkflowId(workflow);
        return submit(workflow, workflowId, Optional.ofNullable(input));
    }
}
```

### Verifying The Results

We can use the `aws cli` tool or the SWF dashboard to verify our results!
Scroll **down** to the bottom until you see the `WorkflowExecutionCompleted` and check out the result!

```json
{
  "events": [
    {
      "eventId": 1,
      "eventType": "WorkflowExecutionStarted",
      "workflowExecutionStartedEventAttributes": {
        "taskList": {
          "name": "DEFAULT"
        },
        "parentInitiatedEventId": 0,
        "taskStartToCloseTimeout": "60",
        "childPolicy": "TERMINATE",
        "executionStartToCloseTimeout": "30758400",
        "input": "\"Jane Doe\"",
        "workflowType": {
          "version": "1.0",
          "name": "Hello World"
        }
      },
      "eventTimestamp": 1456343145.567
    },
    {
      "eventId": 2,
      "eventType": "DecisionTaskScheduled",
      "decisionTaskScheduledEventAttributes": {
        "startToCloseTimeout": "60",
        "taskList": {
          "name": "DEFAULT"
        }
      },
      "eventTimestamp": 1456343145.567
    },
    {
      "eventId": 3,
      "eventType": "DecisionTaskStarted",
      "eventTimestamp": 1456343145.654,
      "decisionTaskStartedEventAttributes": {
        "scheduledEventId": 2,
        "identity": "DECIDER-0"
      }
    },
    {
      "eventId": 4,
      "eventType": "DecisionTaskTimedOut",
      "decisionTaskTimedOutEventAttributes": {
        "startedEventId": 3,
        "timeoutType": "START_TO_CLOSE",
        "scheduledEventId": 2
      },
      "eventTimestamp": 1456343205.66
    },
    {
      "eventId": 5,
      "eventType": "DecisionTaskScheduled",
      "decisionTaskScheduledEventAttributes": {
        "startToCloseTimeout": "60",
        "taskList": {
          "name": "DEFAULT"
        }
      },
      "eventTimestamp": 1456343205.66
    },
    {
      "eventId": 6,
      "eventType": "DecisionTaskStarted",
      "eventTimestamp": 1456343205.708,
      "decisionTaskStartedEventAttributes": {
        "scheduledEventId": 5,
        "identity": "DECIDER-0"
      }
    },
    {
      "eventId": 7,
      "eventType": "DecisionTaskCompleted",
      "decisionTaskCompletedEventAttributes": {
        "startedEventId": 6,
        "scheduledEventId": 5
      },
      "eventTimestamp": 1456343206.078
    },
    {
      "eventId": 8,
      "eventType": "ActivityTaskScheduled",
      "activityTaskScheduledEventAttributes": {
        "taskList": {
          "name": "DEFAULT"
        },
        "scheduleToCloseTimeout": "NONE",
        "activityType": {
          "version": "1.0",
          "name": "Hello World"
        },
        "decisionTaskCompletedEventId": 7,
        "heartbeatTimeout": "NONE",
        "activityId": "step1",
        "scheduleToStartTimeout": "NONE",
        "startToCloseTimeout": "NONE",
        "input": "[ \"[Ljava.lang.Object;\", [ \"Jane Doe\" ] ]"
      },
      "eventTimestamp": 1456343206.078
    },
    {
      "eventId": 9,
      "eventType": "ActivityTaskStarted",
      "eventTimestamp": 1456343206.185,
      "activityTaskStartedEventAttributes": {
        "scheduledEventId": 8,
        "identity": "ACTIVITY-0"
      }
    },
    {
      "eventId": 10,
      "eventType": "ActivityTaskCompleted",
      "activityTaskCompletedEventAttributes": {
        "startedEventId": 9,
        "scheduledEventId": 8,
        "result": "\"Hello World Jane Doe!\""
      },
      "eventTimestamp": 1456343206.39
    },
    {
      "eventId": 11,
      "eventType": "DecisionTaskScheduled",
      "decisionTaskScheduledEventAttributes": {
        "startToCloseTimeout": "60",
        "taskList": {
          "name": "DEFAULT"
        }
      },
      "eventTimestamp": 1456343206.39
    },
    {
      "eventId": 12,
      "eventType": "DecisionTaskStarted",
      "eventTimestamp": 1456343206.45,
      "decisionTaskStartedEventAttributes": {
        "scheduledEventId": 11,
        "identity": "DECIDER-1"
      }
    },
    {
      "eventId": 13,
      "eventType": "DecisionTaskCompleted",
      "decisionTaskCompletedEventAttributes": {
        "startedEventId": 12,
        "scheduledEventId": 11
      },
      "eventTimestamp": 1456343206.621
    },
    {
      "eventId": 14,
      "eventType": "WorkflowExecutionCompleted",
      "workflowExecutionCompletedEventAttributes": {
        "result": "\"Hello World Jane Doe!\"",
        "decisionTaskCompletedEventId": 13
      },
      "eventTimestamp": 1456343206.621
    }
  ]
}
```

That's all it took to get using Simple Workflow Service! No need to fear complicated deciders and distributed work. 
Continue reading the user-guide to learn some more advanced use cases and features of `WaterFlow`.
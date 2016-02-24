# Welcome to WaterFlow

WaterFlow is a non-magical / easy to understand / JDK8 framework for use with [Simple Workflow Service](https://aws.amazon.com/swf/)

## Philosophy

> The framework is deeply routed in the original implementation of [SWiFt](https://bitbucket.org/clarioanalytics/services-swift) which is a great example of achieving a very usable and simple SWF framework.

SWF is a great AWS service allowing the ability to implement distributed, asynchronous applications as workflows. The interesting part of SWF is that its easy to integrate with various *in-house* solutions, since the various actors necessary to operate with the service can operate onsite (only execution history is stored in the "cloud")

The difficulty lies in writing robust, easy and composable workflows. SWF offers the [Flow Framework](https://aws.amazon.com/swf/details/flow/) which is a is a collection of convenience libraries that make it faster and easier to build applications with Amazon Simple Workflow on the JVM. The problem we've found however is that there is just too much **darn magic** !

### No magic - WYSIWYG

A key tenant to the `WaterFlow` framework is the removal of much of the magic present in competing frameworks, while still providing many of the same features. Many times this might lead to code that is *slightly* more verbose, however you are presented with a [WYSIWYG](https://en.wikipedia.org/wiki/WYSIWYG) experience.

### Immutability 

The `WateFlow` framework makes, **heavy** use of the [Immutables](https://immutables.github.io/) library, and so should you! Since typically you might run multiple JVM instances of a particular actor (deciders & activity workers) and each JVM might run several threads - its much easier to think about the underlying primitives knowing they are [immutable](https://docs.oracle.com/javase/tutorial/essential/concurrency/immutable.html).

### Simple Deciders

Deciders are the actors that coordinate the logic of the current workflow by scheduling various actions - such as Activities. The concept of scheduling a future asyncrhonous computation and writing it in a very functional manner is well understood through various *Ayncrhonous Programming Patterns*. `WaterFlow` leverages well known standard interfaces - [CompletionStage](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletionStage.html) - to help making complicated deciders.

### Functional / JDK8

By embracing the `JDK8`, much of the code is written in a very functional manner.

### Tested

The framework includes many **unit** and **integration** tests. These tests serve as great self-documentation - in addition to this site.
# WaterFlow

WaterFlow is a *non-magical* - relatively small framework for use with [Amazon SWF](https://aws.amazon.com/swf/).

# Philosophy

The framework is deeply routed in the original implementation of [SWiFt](https://bitbucket.org/clarioanalytics/services-swift)
which is a great example of achieving a very useable and simple SWF framework.

WaterFlow aims to add new features that are common in the [AWS Flow Framework](https://aws.amazon.com/swf/details/flow/)
without the need for [AspectJ](https://eclipse.org/aspectj/) and the *magical* annotation processing that occurs.

Here is a short list of philosphy decisions that differentiate this framework from SWiFt and [Glisten](https://github.com/Netflix/glisten/)

1. JDK8 target - heavy use of new JDK8 features such as Optional and Streams make for simpler code
2. Allow arbitrary types to be passed as input or sent as output for a given Activity through the use of a `DataConverter`
3. Deciders are written in an asynchronous fashion without any magic!


# Requirements

1. Maven 2 or 3 installation
2. Java 1.8+
3. Active Amazon Web Services account and credentials
4. Access to Amazon SWF with a domain set up
5. Understanding of Amazon Simple Workflow concepts and the Amazon AWS SDK for Java

```
<dependency>
    <groupId>com.github.fzakaria</groupId>
    <artifactId>WaterFlow</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

# Samples

Several examples have been included within the `com.github.fzakaria.waterflow.example` package that demonstrate the full
range of features offered by the framework.
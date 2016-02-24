# Actions

Although `ActivityTask` may be the *bread & butter* of the type of work you can schedule with SWF, there exists many more tools at your disposable.
`WaterFlow` embraces the other actions and makes them available through the same interface you've come to known and love when orchestrating your Workflow/Decider. The following is a ongoing list and examples of the various actions supported by the framework.

All actions ultimately rely on being called via the `public CompletionStage<OutputType> decide(DecisionContext decisionContext)` interface. During replay of the current decider with the current HistoryEvents, `WaterFlow` takes care of fulfilling the `CompletionStage` with any Throwable or Result if the action has completed.

## ActivityAction

A distributed asyncrhonous unit of work that you can call. Depending on the use of a `DataConverter` you can pass and return arbitrary complex POJOs.

```
@ActivityMethod(name = "Hello World", version = "1.0")
public String helloWorld(String name) {
    if (name == null) {
        throw new IllegalArgumentException("You must provide a non null name!");
    }
    return String.format("Hello World %s", name);
}
```

## RecordMarkerAction

You can use markers to record events in the workflow execution history for application specific purposes. Markers are useful when you want to record custom information to help implement decider logic. For instance, another SWF framework [Glisten](https://github.com/Netflix/glisten) uses markers to store log messages - which would give you a single view of all log messages that might have otherwise executed on different machines.

## TimerAction

A timer enables you to notify your decider when a certain amount of time has elapsed. This allows you to orchestrate delays in the workflow before proceeding with the next action.

## WaitForSignalAction

Signals enable you to inform a workflow execution of external events and inject information into a workflow execution while it is running. Workflows can either send or receive signals. This action, allows to orchestrate a workflow that **waits** for a particular signal to be received. One common application for waiting on a signal, might be to model some human interaction that needs to occur as part of the workflow.
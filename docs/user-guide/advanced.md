
## Throwables

> TO DO

## Heartbeat

> TO DO

## Retries

> TO DO

## DataConverters 

The `WaterFlow` framework marshals data across activities & deciders by using a `DataConverter`. By default, the framework offers a data converter that is based on the Jackson JSON processor - similar to the *AWS Flow Framework*.

If the default converter isn't sufficient for your application, you can implement a custom data converter. A good example is offering a `DataConverter` that might encrypt/decrypt data sent over the wire or marshalling data through `S3` to overcome the size limitation of the fields put in palce by SWF
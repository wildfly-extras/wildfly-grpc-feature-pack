# WildFly gRPC

Feature pack to bring gRPC support to WildFly.

## Get Started

To build the feature pack, simply run

```shell
mvn install
```

This will build everything, and run the testsuite. A WildFly server with the gRPC subsystem will be created in
the `build/target` directory.

## Run thw Example

The `example` folder contains the 'Hello World' example from
the [gRPC Java examples](https://github.com/grpc/grpc-java/tree/master/examples).

Build the example, provision a WildFly server with gRPC and start the server in background:

```shell
mvn package wildfly:start -pl example
```

You can use tools like [BloomRPC](https://github.com/uw-labs/bloomrpc)
or [gRPCurl](https://github.com/fullstorydev/grpcurl) to invoke the deployed 'Hello World' gRPC service:

```shell
grpcurl \
  -proto example/src/main/proto/helloworld.proto \
  -plaintext -d '{"name":"Bob"}' \
  localhost:9555 helloworld.Greeter/SayHello
```

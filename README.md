# WildFly gRPC

Feature pack to bring gRPC support to WildFly. Currently, the feature pack supports the deployment of gRPC services annotated with a custom annotation: `org.wildfly.grpc.GrpcService`. 

gRPC services are registered against a gRPC server listening to port 9555. Configuration and customization is not yet supported. Also, only gRPC services are supported at the moment. Support for gRPC clients is coming soon.

# Get Started

To build the feature pack, simply run

```shell
mvn install
```

This will build everything, and run the testsuite. A WildFly server with the gRPC subsystem will be created in
the `build/target` directory.

## Profiles

The maven build supports the following profiles:

- `examples`: Builds the examples
- `testsuite`: Runs the test suite

# Examples

Each example consists of three modules:

1. Proto: Contains the proto definitions 
2. Service: Contains the gRPC service annotated with `@GrpcService`
3. Client: Contains a client to call the deployed gRPC service

Before running the examples, please make sure that all necessary dependencies are available in your local maven repository:

```shell
mvn install -P examples
```

## Hello World

The `helloworld` example is a slightly modified version of the `helloworld` example from [gRPC Java examples](https://github.com/grpc/grpc-java/tree/master/examples).

### Service

To build the `helloworld` service, provision a WildFly server with the gRPC subsystem and deploy the service, run:

```shell
mvn wildfly:run -P examples -pl examples/helloworld/service
```

### Client

The `helloworld` client is a simple Java application. To build the client and call to the gRPC service, run:

```shell
mvn exec:java -P examples -pl examples/helloworld/client -Dexec.args="Bob"
```

Alternatively you could also use tools like [BloomRPC](https://github.com/uw-labs/bloomrpc)
or [gRPCurl](https://github.com/fullstorydev/grpcurl) to invoke the service:

```shell
grpcurl \
  -proto examples/helloworld/proto/src/main/proto/helloworld.proto \
  -plaintext -d '{"name":"Bob"}' \
  localhost:9555 helloworld.Greeter/SayHello
```

## Chat

The `chat` example is taken from [gRPC by example](https://github.com/saturnism/grpc-by-example-java). 

### Service

To build the `chat` service, provision a WildFly server with the gRPC subsystem and deploy the service, run:

```shell
mvn wildfly:run -P examples -pl examples/chat/service
```

### Client

The `chat` client is a JavaFX application. To build the client and connect to the gRPC service, run:

```shell
mvn javafx:run -P examples -pl examples/chat/client
```

To see the `chat` example in action, you should start multiple chat clients. 

# Licenses

This project uses the following licenses:

* [Apache License 2.0](https://repository.jboss.org/licenses/apache-2.0.txt)

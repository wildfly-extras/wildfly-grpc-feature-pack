# WildFly gRPC

Feature pack to bring gRPC support to WildFly.

## Get Started

To build the feature pack, simply run

```shell
mvn install
```

This will build everything, and run the testsuite. A WildFly server with the gRPC subsystem will be created in
the `build/target` directory.

## Examples

### Hello World

The `examples/hello-world` folder contains the 'Hello World' example from
the [gRPC Java examples](https://github.com/grpc/grpc-java/tree/master/examples).

To build the hello-world service, provision a WildFly server with gRPC and start the server, run:

```shell
mvn package wildfly:run -P examples -pl examples/helloworld/service
```
To call the service execute in another shell:

```shell
mvn compile exec:java -P examples -pl examples/helloworld/client -Dexec.args="Bob"
```

Alternatively you could also use tools like [BloomRPC](https://github.com/uw-labs/bloomrpc)
or [gRPCurl](https://github.com/fullstorydev/grpcurl) to invoke the service:

```shell
grpcurl \
  -proto examples/helloworld/proto/src/main/proto/helloworld.proto \
  -plaintext -d '{"name":"Bob"}' \
  localhost:9555 helloworld.Greeter/SayHello
```

### Chat

Pending

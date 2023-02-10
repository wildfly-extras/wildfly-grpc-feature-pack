# WildFly gRPC

Feature pack to bring gRPC support to WildFly. Currently, the feature pack supports the deployment of gRPC services annotated with a custom annotation: `org.wildfly.grpc.GrpcService`. 

gRPC services are registered against a gRPC server listening to port 9555. Only gRPC services are supported at the moment. Support for gRPC clients is coming soon.

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

To build the `helloworld` service, provision a WildFly server with the gRPC subsystem and any necessary certificate files,
and deploy the service, run:

<code>mvn wildfly:run -P examples -pl examples/helloworld/service -Dssl=*SSL*</code>

where *SSL* is either

* none: plaintext
* oneway: server identity is verified
* twoway: both server and client identities are verified

### Client

The `helloworld` client is a simple Java application. To build the client and call to the gRPC service, run:

<code>mvn exec:java -P examples -pl examples/helloworld/client -Dexec.args="Bob *SSL*"</code>

where, again, *SSL* is either "none", "oneway", or "twoway"

Alternatively you could also use tools like [BloomRPC](https://github.com/uw-labs/bloomrpc)
or [gRPCurl](https://github.com/fullstorydev/grpcurl) to invoke the service:

```shell
grpcurl \
  -proto examples/helloworld/proto/src/main/proto/helloworld.proto \
  -plaintext \
  -d '{"name":"Bob"}' \
  localhost:9555 helloworld.Greeter/SayHello
```
or
```shell
grpcurl \
  -proto examples/helloworld/proto/src/main/proto/helloworld.proto \
  -cacert examples/helloworld/client/src/main/resources/client.truststore.pem \
  -d '{"name":"Bob"}' \
  localhost:9555 helloworld.Greeter/SayHello
```
or
```shell
grpcurl \
  -proto examples/helloworld/proto/src/main/proto/helloworld.proto \
  -cacert examples/helloworld/client/src/main/resources/client.truststore.pem \
  -cert examples/helloworld/client/src/main/resources/client.keystore.pem \
  -key examples/helloworld/client/src/main/resources/client.key.pem \
  -d '{"name":"Bob"}' \
  localhost:9555 helloworld.Greeter/SayHello
```
**Note.** To use the current versions of the certificate files with grpcurl, it is necessary to set

   <code>export GODEBUG=x509ignoreCN=0</code>

This restriction will be removed in the future.

## Chat

The `chat` example is taken from [gRPC by example](https://github.com/saturnism/grpc-by-example-java). 

### Service

To build the `chat` service, provision a WildFly server with the gRPC subsystem and any necessary certificate files,
and deploy the service, run:

<code>mvn wildfly:run -P examples -pl examples/chat/service -Dssl=*SSL*</code>

where *SSL* is either

* none: plaintext
* oneway: server identity is verified
* twoway: both server and client identities are verified


### Client

The `chat` client is a JavaFX application. To build the client and connect to the gRPC service, run:

<code>mvn javafx:run -P examples -pl examples/chat/client -Dexec.args="*SSL*"</code>

To see the `chat` example in action, you should start multiple chat clients. 

# Licenses

This project uses the following licenses:

* [Apache License 2.0](https://repository.jboss.org/licenses/apache-2.0.txt)

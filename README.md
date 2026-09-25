# WildFly gRPC

Feature pack to bring gRPC support to WildFly. gRPC services are served via WildFly's Undertow HTTP/2 stack — no separate port is needed. By default, gRPC is available on the standard HTTP listener (port 8080) with HTTP/2 cleartext (h2c) enabled automatically, and on any configured HTTPS listener (port 8443) via ALPN.

Only gRPC services are supported at the moment. Support for gRPC clients is coming soon.

**Note:** Starting with version 0.1.17, the group ID has changed from `org.wildfly.extras.grpc` to `org.wildfly.grpc`.

# Get Started

To build the feature pack, simply run

```shell
mvn install
```

This will build everything, and run the testsuite.

Once built you can provision a server with gRPC support using Galleon provisioning. An example using the 
`org.wildfly.plugins:wildfly-maven-plugin`:

```xml
<plugin>
    <groupId>org.wildfly.plugins</groupId>
    <artifactId>wildfly-maven-plugin</artifactId>
    <configuration>
        <feature-packs>
            <feature-pack>
                <groupId>org.wildfly</groupId>
                <artifactId>wildfly-galleon-pack</artifactId>
                <version>${version.wildfly}</version>
            </feature-pack>
            <feature-pack>
                <groupId>org.wildfly.grpc</groupId>
                <artifactId>wildfly-grpc-feature-pack</artifactId>
                <version>${version.wildfly.grpc}</version>
            </feature-pack>
        </feature-packs>
        <layers>
            <layer>jaxrs-server</layer>
            <layer>grpc</layer>
        </layers>
        <galleon-options>
            <jboss-fork-embedded>${galleon.fork.embedded}</jboss-fork-embedded>
            <stability-level>preview</stability-level>
        </galleon-options>
        <stability>preview</stability>
        <provisioning-dir>wildfly</provisioning-dir>
        <log-provisioning-time>${galleon.log.time}</log-provisioning-time>
        <offline>true</offline>
    </configuration>
</plugin>
```

You can also configure this with the Galleon CLI tool:

```bash
galleon.sh install org.wildfly:wildfly-galleon-pack:$WILDFLY_VERSION --dir=wildfly-grpc
galleon.sh install org.wildfly.grpc:wildfly-grpc-feature-pack:$GRPC_VERSION --dir=wildfly-grpc
```

# Examples

Each example consists of three modules:

1. Proto: Contains the proto definitions 
2. Service: Contains the gRPC service
3. Client: Contains a client to call the deployed gRPC service

Before running the examples, please make sure that all necessary dependencies are available in your local maven repository:

```shell
mvn install -P examples
```

## Hello World

The `helloworld` example is a slightly modified version of the `helloworld` example from [gRPC Java examples](https://github.com/grpc/grpc-java/tree/master/examples).

### Service

From the `examples/helloworld/service` directory, build and provision the server with the gRPC service pre-deployed:

```shell
cd examples/helloworld/service
mvn clean package -Dssl=none
```

Then start WildFly:

```shell
./target/wildfly/bin/standalone.sh --stability=preview
```

For TLS variants, pass `-Dssl=oneway` or `-Dssl=twoway` to `mvn clean package` instead.

### Client

The `helloworld` client is a simple Java application. From the project root, run:

<code>mvn exec:java -P examples -pl examples/helloworld/client -Dexec.args="Bob *SSL*"</code>

where *SSL* is either "none", "oneway", or "twoway".

Alternatively, use [grpcurl](https://github.com/fullstorydev/grpcurl) to invoke the service directly.
From the `examples/helloworld/service` directory, pass the proto file with `-import-path` and `-proto`
since the server does not expose the gRPC reflection API:

```shell
cd examples/helloworld/service

# plaintext (port 8080, h2c)
grpcurl \
  -plaintext \
  -import-path ../proto/src/main/proto \
  -proto helloworld.proto \
  -d '{"name":"Bob"}' \
  localhost:8080 helloworld.Greeter/SayHello

# oneway TLS (port 8443)
grpcurl \
  -cacert ../../../ssl/server.pem \
  -import-path ../proto/src/main/proto \
  -proto helloworld.proto \
  -d '{"name":"Bob"}' \
  localhost:8443 helloworld.Greeter/SayHello

# twoway TLS (port 8443)
grpcurl \
  -cacert ../../../ssl/server.pem \
  -cert ../client/src/main/resources/client.keystore.pem \
  -key ../client/src/main/resources/client.key.pem \
  -import-path ../proto/src/main/proto \
  -proto helloworld.proto \
  -d '{"name":"Bob"}' \
  localhost:8443 helloworld.Greeter/SayHello
```

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

# Feature Pack Documentation

This feature pack uses the [WildFly Galleon Plugins](https://github.com/wildfly/galleon-plugins) to publish its 
feature pack documentation. The documentation is available using the following links:

- https://wildfly-extras.github.io/wildfly-grpc-feature-pack/ - Contains the latest documentation
- https&#8203;://wildfly-extras.github.io/wildfly-grpc-feature-pack/&lt;sem-version&gt; - Contains the documentation for a specific version

# Licenses

This project uses the following licenses:

* [Apache License 2.0](https://repository.jboss.org/licenses/apache-2.0.txt)

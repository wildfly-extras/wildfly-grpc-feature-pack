# WildFly gRPC

Feature pack to bring gRPC support to WildFly. gRPC services are served via WildFly's Undertow HTTP/2 stack — no separate port is needed. The `grpc` Galleon layer enables HTTP/2 cleartext (h2c) on the standard HTTP listener (port 8080) automatically. For TLS (port 8443), configure an Undertow `https-listener` with `enable-http2="true"` — HTTP/2 is not enabled on HTTPS listeners by default even though TLS supports ALPN.

Only gRPC services are supported at the moment. Support for gRPC clients is coming soon.

**Note:** Starting with version 0.1.17, the group ID has changed from `org.wildfly.extras.grpc` to `org.wildfly.grpc`.

# Get Started

To build the feature pack, simply run

```shell
mvn install
```

This will build everything, and run the testsuite. The build includes the `ssl-gen` module that generates self-signed SSL certificates used by the integration tests and examples for TLS testing.

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

Each example consists of three modules: proto definitions, a gRPC service deployed to WildFly, and a client.

Before running the examples, build the project first so that SSL certificates and the feature pack are available:

Before running the examples, please make sure that all necessary dependencies are available in your local maven repository:

```shell
mvn install
```

## Hello World

The `helloworld` example is a slightly modified version of the `helloworld` example from [gRPC Java examples](https://github.com/grpc/grpc-java/tree/master/examples).

### Service

From the `examples/helloworld/service` directory, build and provision the server with the gRPC service pre-deployed:

```shell
cd examples/helloworld/service
mvn clean package
```

Then start WildFly:

```shell
./target/wildfly/bin/standalone.sh --stability=preview
```

The server is provisioned with both listeners ready:
- **Port 8080** — HTTP/2 cleartext (h2c), no certificate required
- **Port 8443** — HTTPS with HTTP/2 via ALPN; the SSL context uses `want-client-auth=true` and `authentication-optional=true` so clients may optionally present a certificate for mutual TLS

### Client

The `helloworld` client is a simple Java application. From the project root, run:

<code>mvn exec:java -pl examples/helloworld/client -Dexec.args="Bob *SSL*"</code>

where *SSL* is either "none" (port 8080, h2c), "oneway" (port 8443, TLS), or "twoway" (port 8443, mutual TLS).

Alternatively, use [grpcurl](https://github.com/fullstorydev/grpcurl) to invoke the service directly.
From the `examples/helloworld` directory, pass the proto file with `-import-path` and `-proto`
since the server does not expose the gRPC reflection API:

```shell
cd examples/helloworld

# plaintext (port 8080, h2c)
grpcurl \
  -plaintext \
  -import-path proto/src/main/proto \
  -proto helloworld.proto \
  -d '{"name":"Bob"}' \
  localhost:8080 helloworld.Greeter/SayHello

# TLS, one-way (port 8443) — server authenticates to client, no client cert
grpcurl \
  -cacert ../../ssl-gen/target/generated-ssl/ca.pem \
  -import-path proto/src/main/proto \
  -proto helloworld.proto \
  -d '{"name":"Bob"}' \
  localhost:8443 helloworld.Greeter/SayHello

# TLS, two-way (port 8443) — mutual authentication, client presents a certificate
grpcurl \
  -cacert ../../ssl-gen/target/generated-ssl/ca.pem \
  -cert ../../ssl-gen/target/generated-ssl/client.keystore.pem \
  -key ../../ssl-gen/target/generated-ssl/client.key.pem \
  -import-path proto/src/main/proto \
  -proto helloworld.proto \
  -d '{"name":"Bob"}' \
  localhost:8443 helloworld.Greeter/SayHello
```

## Chat

The `chat` example demonstrates bidirectional streaming gRPC. The client is a terminal UI (TUI) application built with [TamboUI](https://tamboui.dev/).

See [examples/chat/README.md](examples/chat/README.md) for instructions on building and running the service and client.

# Feature Pack Documentation

This feature pack uses the [WildFly Galleon Plugins](https://github.com/wildfly/galleon-plugins) to publish its 
feature pack documentation. The documentation is available using the following links:

- https://wildfly-extras.github.io/wildfly-grpc-feature-pack/ - Contains the latest documentation
- https&#8203;://wildfly-extras.github.io/wildfly-grpc-feature-pack/&lt;sem-version&gt; - Contains the documentation for a specific version

# Licenses

This project uses the following licenses:

* [Apache License 2.0](https://repository.jboss.org/licenses/apache-2.0.txt)

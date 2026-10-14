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

Each example consists of three modules:

1. Proto: Contains the proto definitions 
2. Service: Contains the gRPC service
3. Client: Contains a client to call the deployed gRPC service

## Hello World

The `helloworld` example is a slightly modified version of the `helloworld` example from [gRPC Java examples](https://github.com/grpc/grpc-java/tree/master/examples).

See [examples/helloworld/README.md](examples/helloworld/README.md) for instructions on building and running the service and client.

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

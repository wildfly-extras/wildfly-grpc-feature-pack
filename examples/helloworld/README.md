# Hello World

The `helloworld` example is a slightly modified version of the `helloworld` example from [gRPC Java examples](https://github.com/grpc/grpc-java/tree/master/examples).

## Prerequisites

Build the whole project first so that SSL certificates and the feature pack are available:

```shell
mvn install
```

## Service

From the `examples/helloworld/service` directory, build and provision the server with the gRPC service pre-deployed:

```shell
cd examples/helloworld/service
mvn clean package
```

This provisions a WildFly server with:
- **Port 8080** — HTTP/2 cleartext (h2c)
- **Port 8443** — HTTPS with HTTP/2 via ALPN (one-way TLS)

For mutual TLS on port 8443, pass `-Dssl=twoway`:

```shell
mvn clean package -Dssl=twoway
```

Then start WildFly:

```shell
./target/wildfly/bin/standalone.sh --stability=preview
```

## Client

The `helloworld` client is a simple Java application. From the project root, run:

```shell
mvn exec:java -pl examples/helloworld/client -Dexec.args="Bob none"    # plaintext (port 8080)
mvn exec:java -pl examples/helloworld/client -Dexec.args="Bob oneway"  # TLS (port 8443)
mvn exec:java -pl examples/helloworld/client -Dexec.args="Bob twoway"  # mutual TLS (port 8443)
```

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
  -cacert ../../ssl-gen/target/generated-ssl/client.truststore.pem \
  -import-path proto/src/main/proto \
  -proto helloworld.proto \
  -d '{"name":"Bob"}' \
  localhost:8443 helloworld.Greeter/SayHello

# TLS, two-way (port 8443) — mutual authentication (requires -Dssl=twoway on service build)
grpcurl \
  -cacert ../../ssl-gen/target/generated-ssl/client.truststore.pem \
  -cert ../../ssl-gen/target/generated-ssl/client.crt.pem \
  -key ../../ssl-gen/target/generated-ssl/client.key.pem \
  -import-path proto/src/main/proto \
  -proto helloworld.proto \
  -d '{"name":"Bob"}' \
  localhost:8443 helloworld.Greeter/SayHello
```

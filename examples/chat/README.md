# Chat

The `chat` example demonstrates a bidirectional streaming gRPC service: clients stream `ChatMessage` requests and receive `ChatMessageFromServer` responses in real time from all connected participants.

## Service

To build the `chat` service, provision a WildFly server with the gRPC subsystem and any necessary certificate files,
and deploy the service, run:

```shell
cd service
mvn clean package -Dssl=<*SSL*>
```

where *SSL* is either

* none: plaintext
* oneway: server identity is verified
* twoway: both server and client identities are verified

Then, run the server with:

```shell
./target/wildfly/bin/standalone.sh --stability preview
```

## Client

The `chat` client is a JavaFX desktop application. To build and launch it, run:

```shell
cd client
mvn package
mvn javafx:run -Dexec.args="*SSL*"
```

where, again, *SSL* is either "none", "oneway", or "twoway"

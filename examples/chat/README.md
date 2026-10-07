# Chat

The `chat` example demonstrates a bidirectional streaming gRPC service: clients stream `ChatMessage` requests and receive `ChatMessageFromServer` responses in real time from all connected participants.

The client is a terminal UI (TUI) application built with [TamboUI](https://tamboui.dev/).

## Service

To build the `chat` service, provision a WildFly server with the gRPC subsystem and any necessary certificate files,
and deploy the service, run:

```shell
cd service
mvn clean package -Dssl=<SSL>
```

where `SSL` is either

* `none`: plaintext
* `oneway`: server identity is verified
* `twoway`: both server and client identities are verified

Then, run the server with:

```shell
./target/wildfly/bin/standalone.sh --stability preview
```

## Client

The `chat` client is a TamboUI terminal application. To build it, run:

```shell
cd client
mvn package
```

This produces a fat JAR that can be launched directly:

```shell
java -jar target/chat-client.jar [options] [username]
```

### Command Line Arguments

| Argument | Default | Description |
|----------|---------|-------------|
| `--ssl=<mode>` | `none` | SSL mode: `none`, `oneway`, or `twoway` (defaults to `none`, must match the server) |
| `username` | `User<pid>` | Chat username (defaults to `User` followed by the process ID) |

### Example: Multi-User Chat

Start the server in one terminal:

```shell
cd service
mvn clean package -Dssl=none
./target/wildfly/bin/standalone.sh --stability preview
```

Start Alice in a second terminal:

```shell
cd client
java -jar target/chat-client.jar Alice
```

Start Bob in a third terminal:

```shell
cd client
java -jar target/chat-client.jar Bob
```

Messages sent by Alice will appear in Bob's terminal and vice versa.

### Controls

* **Tab** — switch focus between Name and Message fields
* **Enter** — send message
* **Ctrl+C** — quit

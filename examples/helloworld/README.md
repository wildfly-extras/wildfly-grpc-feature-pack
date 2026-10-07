# Hello World

The `helloworld` example is a slightly modified version of the `helloworld` example from [gRPC Java examples](https://github.com/grpc/grpc-java/tree/master/examples).

## Service

To build the `helloworld` service, provision a WildFly server with the gRPC subsystem and any necessary certificate files,
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

The `helloworld` client is a simple Java application. To build it, run:

```shell
cd client
mvn package
```

This produces a fat JAR that can be launched directly:

```shell
java -jar target/helloworld-client.jar [options] [name]
```

### Command Line Arguments

| Argument | Default | Description |
|----------|---------|-------------|
| `--ssl=<mode>` | `none` | SSL mode: `none`, `oneway`, or `twoway` (defaults to `none`, must match the server) |
| `name` | `world` | Name to greet (defaults to `world`) |

### Examples

Start the server in one terminal:

```shell
cd service
mvn clean package -Dssl=none
./target/wildfly/bin/standalone.sh --stability preview
```

Run the client in another terminal:

```shell
cd client
java -jar target/helloworld-client.jar Bob
```

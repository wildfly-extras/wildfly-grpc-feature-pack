# Hello World

The `helloworld` example is a slightly modified version of the `helloworld` example from [gRPC Java examples](https://github.com/grpc/grpc-java/tree/master/examples).

## Service

To build the `helloworld` service, provision a WildFly server with the gRPC subsystem and any necessary certificate files,
and deploy the service, run:

```shell
<<<<<<< HEAD
cd service
=======
cd examples/helloworld/service
>>>>>>> 5924897 (Move helloworld instructions to examples/helloworld/README.md)
mvn clean package -Dssl=<*SSL*>
```

where *SSL* is either

* none: plaintext
* oneway: server identity is verified
* twoway: both server and client identities are verified

Then, run the server with:

```shell
<<<<<<< HEAD
./target/wildfly/bin/standalone.sh --stability preview
=======
./target/bin/standalone.sh --stability preview
>>>>>>> 5924897 (Move helloworld instructions to examples/helloworld/README.md)
```

## Client

The `helloworld` client is a simple Java application. To build the client and call to the gRPC service, run:

```shell
<<<<<<< HEAD
cd client
=======
cd  examples/helloworld/client
>>>>>>> 5924897 (Move helloworld instructions to examples/helloworld/README.md)
mvn package
mvn exec:java -Dexec.args="Bob *SSL*"
```
where, again, *SSL* is either "none", "oneway", or "twoway"

Alternatively you could also use tools like [BloomRPC](https://github.com/uw-labs/bloomrpc)
or [gRPCurl](https://github.com/fullstorydev/grpcurl) to invoke the service:

```shell
# clear text connection
<<<<<<< HEAD
grpcurl -proto proto/src/main/proto/helloworld.proto \
=======
grpcurl -proto ../proto/src/main/proto/helloworld.proto \
>>>>>>> 5924897 (Move helloworld instructions to examples/helloworld/README.md)
  -plaintext \
  -d '{"name":"Bob"}' \
  localhost:9555 helloworld.Greeter/SayHello
```
or
```shell
# tls (oneway)
<<<<<<< HEAD
grpcurl -proto proto/src/main/proto/helloworld.proto \
  -cacert ../../ssl-gen/target/generated-ssl/client.truststore.pem \
=======
grpcurl -proto ../proto/src/main/proto/helloworld.proto \
  -cacert ../../../ssl-gen/target/generated-ssl/client.truststore.pem \
>>>>>>> 5924897 (Move helloworld instructions to examples/helloworld/README.md)
  -d '{"name":"Bob"}' \
  localhost:9555 helloworld.Greeter/SayHello
```
or
```shell
# mutual TLS (twoway)
<<<<<<< HEAD
grpcurl -proto proto/src/main/proto/helloworld.proto \
  -cacert ../../ssl-gen/target/generated-ssl/client.truststore.pem \
  -cert ../../ssl-gen/target/generated-ssl/client.crt.pem \
  -key ../../ssl-gen/target/generated-ssl/client.key.pem \
=======
grpcurl -proto ../proto/src/main/proto/helloworld.proto \
  -cacert ../../../ssl-gen/target/generated-ssl/client.truststore.pem \
  -cert ../../../ssl-gen/target/generated-ssl/client.crt.pem \
  -key ../../../ssl-gen/target/generated-ssl/client.key.pem \
>>>>>>> 5924897 (Move helloworld instructions to examples/helloworld/README.md)
  -d '{"name":"Bob"}' \
  localhost:9555 helloworld.Greeter/SayHello
```
**Note.** To use the current versions of the certificate files with grpcurl, it is necessary to set

   <code>export GODEBUG=x509ignoreCN=0</code>

This restriction will be removed in the future.

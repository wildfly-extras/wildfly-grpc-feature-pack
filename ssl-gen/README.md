# WildFly gRPC SSL Certificate Generator

The `ssl-gen` module generates self-signed SSL certificates and Elytron configuration scripts at build time, for use by the integration tests and examples when testing TLS-enabled gRPC in WildFly.

All artifacts are generated into `ssl-gen/target/generated-ssl/` during the Maven `generate-resources` / `prepare-package` phases and consumed by other modules via Maven resource copying — they are never checked into source control.

## Generated files

### Server-side

| File | Format | Purpose |
|------|--------|---------|
| `server.keystore.p12` | PKCS12 | Server keypair (CN=localhost, SAN=dns:localhost,ip:127.0.0.1). Loaded by WildFly Elytron for one-way and mutual TLS. |
| `server.truststore.p12` | PKCS12 | Server truststore containing the trusted client certificate. Used by WildFly Elytron for mutual TLS (mTLS). |
| `configure-elytron-tls.cli` | JBoss CLI | Configures one-way TLS in WildFly: creates a key-store, key-manager, and server-ssl-context, then sets `key-manager-name` on the gRPC subsystem. |
| `configure-elytron-mutual-tls.cli` | JBoss CLI | Extends one-way TLS to mutual TLS: adds a trust-store and trust-manager, sets `need-client-auth=true`, and sets `trust-manager-name` on the gRPC subsystem. Run after `configure-elytron-tls.cli`. |

### Client-side

| File | Format | Purpose |
|------|--------|---------|
| `client.keystore.p12` | PKCS12 | Client keypair (CN=client). Used by gRPC Java clients for mutual TLS. |
| `client.truststore.p12` | PKCS12 | Client truststore containing the trusted server certificate. |
| `client.truststore.pem` | PEM | Server certificate in PEM format. Used by gRPC Java's `SslContextBuilder.trustManager(InputStream)` for one-way TLS. |
| `client.crt.pem` | PEM | Client certificate in PEM format. Used by `grpcurl` for mutual TLS (`-cert` flag). |
| `client.key.pem` | PEM | Client private key in PEM format. Used by `grpcurl` for mutual TLS (`-key` flag). |

### Intermediate files (not consumed directly)

| File | Purpose |
|------|--------|
| `server.cer` | Server DER certificate, imported into the client truststore during build. |
| `client.cer` | Client DER certificate, imported into the server truststore during build. |

## Where files are consumed

| Consumer | Files used |
|----------|-----------|
| `examples/helloworld/service` | `server.keystore.p12`, `server.truststore.p12`, `configure-elytron-tls.cli`, `configure-elytron-mutual-tls.cli` |
| `examples/helloworld/client` | `client.keystore.p12`, `client.truststore.p12`, `client.truststore.pem` |
| `examples/chat/service` | `server.keystore.p12`, `server.truststore.p12`, `configure-elytron-tls.cli`, `configure-elytron-mutual-tls.cli` |
| `examples/chat/client` | `client.keystore.p12`, `client.truststore.p12`, `client.truststore.pem` |
| `testsuite/integration/subsystem` | `server.keystore.p12`, `server.truststore.p12`, `client.keystore.p12`, `client.truststore.p12`, `client.truststore.pem` |

## Certificate details

All certificates use RSA 2048-bit keys, are valid for 365 days, and share the same password: `secret`.

The server certificate includes a Subject Alternative Name (SAN) for `localhost` and `127.0.0.1`, which is required for modern TLS hostname verification in gRPC.

## Java class

`ExportClientPem` (run via `exec-maven-plugin`) reads `client.keystore.p12` using the BouncyCastle library and writes the client certificate and private key as separate PEM files (`client.crt.pem` and `client.key.pem`). This is needed because `keytool` cannot export private keys directly.

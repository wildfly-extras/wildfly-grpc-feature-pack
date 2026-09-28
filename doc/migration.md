# Migration from Netty-based gRPC Subsystem

The gRPC subsystem was rewritten to route traffic through WildFly's existing Undertow HTTP/2
stack rather than opening a separate Netty TCP socket. As a result, transport-level settings
that were previously configured in the `grpc` subsystem are now the responsibility of the
Undertow `http-listener` or `https-listener`. This document explains what happened to each
attribute that was removed.

## Port change

The old implementation opened a dedicated Netty TCP socket, by default on port **9555**
(configured via the `socket-binding` attribute). In the new implementation, gRPC traffic
is served by WildFly's existing Undertow listeners on the standard ports:

| Protocol | Listener | Default port |
|---|---|---|
| Plaintext (h2c) | `http-listener` | 8080 |
| TLS | `https-listener` | 8443 |

**gRPC clients must be updated to connect to port 8080 (plaintext) or 8443 (TLS) instead of 9555.**

## Plaintext connections — h2c

gRPC over a plaintext connection uses HTTP/2 cleartext (h2c). The WildFly gRPC Galleon
feature group automatically sets `enable-http2="true"` on the default `http-listener`
when the feature pack is provisioned — no manual configuration is required.

gRPC clients connect with `.usePlaintext()`:
```java
ManagedChannel channel = ManagedChannelBuilder.forTarget("localhost:8080")
        .usePlaintext()
        .build();
```

## TLS connections — https-listener

WildFly's default standalone configuration does not include an `https-listener`. To serve
gRPC over TLS, you must create one explicitly and attach an Elytron `ServerSSLContext` to it.
HTTP/2 must also be enabled on the https-listener.

```xml
<!-- 1. Create an Elytron SSL context (key store, key manager, ssl context) -->
<subsystem xmlns="urn:wildfly:elytron:18.0">
    <tls>
        <key-stores>
            <key-store name="grpc-key-store">
                <credential-reference clear-text="secret"/>
                <implementation type="JKS"/>
                <file path="server.keystore.jks" relative-to="jboss.server.config.dir"/>
            </key-store>
        </key-stores>
        <key-managers>
            <key-manager name="grpc-key-manager" key-store="grpc-key-store">
                <credential-reference clear-text="secret"/>
            </key-manager>
        </key-managers>
        <server-ssl-contexts>
            <server-ssl-context name="grpc-ssl-context" key-manager="grpc-key-manager"/>
        </server-ssl-contexts>
    </tls>
</subsystem>

<!-- 2. Add an https-listener referencing the SSL context, with HTTP/2 enabled -->
<subsystem xmlns="urn:jboss:domain:undertow:15.0">
    <server name="default-server">
        <https-listener name="https" socket-binding="https"
                        ssl-context="grpc-ssl-context"
                        enable-http2="true"/>
    </server>
</subsystem>
```

The feature pack ships a ready-made CLI script that configures the full Elytron TLS chain
(key store, key manager, trust manager, SSL context) and adds the https-listener in one step:

```bash
$JBOSS_HOME/bin/jboss-cli.sh --connect --file=ssl/configure-elytron-tls.cli
```

Alternatively, add only the https-listener via the CLI:
```
/subsystem=undertow/server=default-server/https-listener=https:add(
    socket-binding=https, ssl-context=grpc-ssl-context, enable-http2=true)
```

gRPC clients connect with TLS credentials:
```java
ChannelCredentials creds = TlsChannelCredentials.newBuilder()
        .trustManager(truststoreStream)
        .build();
ManagedChannel channel = Grpc.newChannelBuilderForAddress("localhost", 8443, creds).build();
```

## Attributes moved to the Undertow subsystem

Configure these on the `<http-listener>` (plaintext) or `<https-listener>` (TLS) in the
`undertow` subsystem instead.

| Old gRPC attribute | Undertow attribute | Element |
|---|---|---|
| `flow-control-window` | `http2-initial-window-size` | `<http-listener>` / `<https-listener>` |
| `initial-flow-control-window` | `http2-initial-window-size` | `<http-listener>` / `<https-listener>` |
| `max-concurrent-calls-per-connection` | `http2-max-concurrent-streams` | `<http-listener>` / `<https-listener>` |
| `ssl-context-name` | `ssl-context` (Elytron SSLContext) | `<https-listener>` |
| `key-manager-name` | included in the Elytron `ssl-context` | `<https-listener>` |
| `trust-manager-name` | included in the Elytron `ssl-context` | `<https-listener>` |
| `session-cache-size` | `ssl-session-cache-size` | `<https-listener>` |
| `session-timeout` | `ssl-session-timeout` | `<https-listener>` |

### Example: flow control window

Old configuration in `standalone.xml`:
```xml
<subsystem xmlns="urn:wildfly:grpc:preview:1.0">
    <flow-control-window value="65536"/>
</subsystem>
```

New configuration:
```xml
<subsystem xmlns="urn:jboss:domain:undertow:15.0">
    <server name="default-server">
        <http-listener name="default" socket-binding="http"
                       enable-http2="true"
                       http2-initial-window-size="65536"/>
    </server>
</subsystem>
```

### Example: TLS (ssl-context-name / key-manager-name / trust-manager-name)

Old configuration:
```xml
<subsystem xmlns="urn:wildfly:grpc:preview:1.0">
    <key-manager-name value="grpcKM"/>
    <trust-manager-name value="grpcTM"/>
</subsystem>
```

New configuration — configure an Elytron SSLContext and reference it from the https-listener:
```xml
<subsystem xmlns="urn:wildfly:elytron:18.0">
    <tls>
        <key-managers>
            <key-manager name="grpcKM" .../>
        </key-managers>
        <trust-managers>
            <trust-manager name="grpcTM" .../>
        </trust-managers>
        <server-ssl-contexts>
            <server-ssl-context name="grpcSSL" key-manager="grpcKM" trust-manager="grpcTM"/>
        </server-ssl-contexts>
    </tls>
</subsystem>

<subsystem xmlns="urn:jboss:domain:undertow:15.0">
    <server name="default-server">
        <https-listener name="https" socket-binding="https" ssl-context="grpcSSL"/>
    </server>
</subsystem>
```

## Attributes not applicable to the servlet model

The following attributes controlled gRPC/Netty-level PING frames and connection lifecycle
features that are specific to the Netty transport. There is no equivalent in the servlet/Undertow
model because Undertow delegates connection management to the XNIO I/O layer at a different
granularity.

| Old gRPC attribute | Notes |
|---|---|
| `keep-alive-time` | gRPC HTTP/2 PING interval — not configurable in servlet containers |
| `keep-alive-timeout` | gRPC PING timeout — same reason |
| `max-connection-age` | Netty per-connection maximum lifetime — no Undertow equivalent |
| `max-connection-age-grace` | Netty grace period after `max-connection-age` — no Undertow equivalent |
| `max-connection-idle` | Netty idle connection closing — closest Undertow setting is `no-request-timeout` on the listener, but the semantics differ |
| `permit-keep-alive-time` | gRPC server-side PING policy — not applicable |
| `permit-keep-alive-without-calls` | gRPC server-side PING policy — not applicable |
| `handshake-timeout` | Netty TLS handshake timeout — no Undertow equivalent |
| `protocol-provider` | Netty `SslProvider` enum — not applicable to Undertow/servlet |

## Attributes removed as part of the architecture change

| Old gRPC attribute | Reason |
|---|---|
| `socket-binding` | The gRPC server no longer opens its own TCP port. Use `server-name` + `virtual-host` to select which Undertow virtual host serves gRPC traffic. |
| `start-tls` | Use `<http-listener>` for cleartext (h2c) and `<https-listener>` for TLS. |
| `shutdown-timeout` | `GrpcServlet.destroy()` has no configurable shutdown timeout. |

## Attributes retained in the gRPC subsystem

These two attributes remain in the `grpc` subsystem because they are gRPC-protocol-level
settings (not transport-level) controlled directly by `ServletServerBuilder`:

| Attribute | Default | Description |
|---|---|---|
| `max-inbound-message-size` | 4194304 (4 MB) | Maximum size of a single incoming gRPC message |
| `max-inbound-metadata-size` | 8192 (8 KB) | Maximum size of gRPC request headers/metadata |

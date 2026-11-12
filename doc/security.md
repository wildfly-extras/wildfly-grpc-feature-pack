# Security and Identity Propagation for gRPC Services

## Background

Regular WildFly servlet deployments receive security identity propagation automatically:
`ElytronContextAssociationHandler` authenticates the HTTP request and wraps the handler
chain with `SecurityIdentity.runAs()` so that any code executing on the request thread can
call `SecurityDomain.getCurrent().getCurrentSecurityIdentity()` and get the caller's identity.

In the servlet-based gRPC architecture, each WAR that contains gRPC services gets a
`GrpcServlet` embedded directly into the WAR's deployment. Because the gRPC servlet lives
inside the WAR's handler chain, WildFly's normal security pipeline — including
`ElytronContextAssociationHandler` — runs for every gRPC request. However, gRPC service
methods execute on a gRPC executor thread, not the Undertow request thread, so the
Elytron security context is not automatically available to service code. The subsystem
bridges this gap through two cooperating components.

---

## Configuration

Security is configured **per-WAR**, using WildFly's standard Undertow security model.
No subsystem-level security attribute is needed.

### 1. Configure an Elytron security domain

Create an Elytron realm, security domain, and HTTP authentication factory:

```
# Create a filesystem realm with users
/subsystem=elytron/filesystem-realm=grpc-realm:add(path=grpc-realm, relative-to=jboss.server.data.dir)
/subsystem=elytron/filesystem-realm=grpc-realm:add-identity(identity=alice)
/subsystem=elytron/filesystem-realm=grpc-realm:set-password(identity=alice, clear={password=secret})
/subsystem=elytron/filesystem-realm=grpc-realm:add-identity-attribute(identity=alice, name=groups, value=[grpc-user])

# Create a security domain with a role decoder
/subsystem=elytron/simple-role-decoder=grpc-role-decoder:add(attribute=groups)
/subsystem=elytron/security-domain=grpc-domain:add(
    default-realm=grpc-realm,
    realms=[{realm=grpc-realm, role-decoder=grpc-role-decoder}],
    permission-mapper=default-permission-mapper)

# Create an HTTP authentication factory with BASIC mechanism
/subsystem=elytron/http-authentication-factory=grpc-http-auth:add(
    http-server-mechanism-factory=global,
    security-domain=grpc-domain,
    mechanism-configurations=[{
        mechanism-name=BASIC,
        mechanism-realm-configurations=[{realm-name=grpc-realm}]
    }])
```

### 2. Map an Undertow application-security-domain

Wire the HTTP authentication factory into Undertow. Setting `override-deployment-config=true`
forces `PRO_ACTIVE` authentication mode so credentials in HTTP headers (including gRPC
metadata) are processed on every request, even without `<security-constraint>` elements:

```
/subsystem=undertow/application-security-domain=grpc-domain:add(
    http-authentication-factory=grpc-http-auth,
    override-deployment-config=true)
```

### 3. Declare the security domain in the WAR

Add a `jboss-web.xml` to the WAR's `WEB-INF/`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jboss-web>
    <security-domain>grpc-domain</security-domain>
</jboss-web>
```

And a `web.xml` with a `<login-config>`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app xmlns="https://jakarta.ee/xml/ns/jakartaee" version="6.0">
    <login-config>
        <auth-method>BASIC</auth-method>
        <realm-name>grpc-realm</realm-name>
    </login-config>
</web-app>
```

This is the same configuration used for any WildFly servlet deployment — nothing
gRPC-specific is required.

---

## How Identity Is Propagated

When a WAR has a security domain configured, two components cooperate to bridge the
authenticated identity from the Undertow request thread to the gRPC executor thread:

### 1. Inner Handler Chain Wrapper (`GrpcServletInstallProcessor`)

`GrpcServletInstallProcessor` installs an inner handler chain wrapper at the front of
the WAR's handler chain. This wrapper runs on the Undertow request thread, **inside** the
scope of `ElytronRunAsHandler`, so `SecurityDomain.getCurrent()` returns the caller's
authenticated identity.

The wrapper:
1. Reads the identity from `SecurityDomain.getCurrent().getCurrentSecurityIdentity()`
2. For mTLS deployments where `ElytronContextAssociationHandler` runs in `CONSTRAINT_DRIVEN`
   mode (not `PRO_ACTIVE`), falls back to explicit client certificate authentication via
   `ServerAuthenticationContext.setAuthenticationPrincipal()` using the X.509 certificate
   from the TLS session
3. Stores the identity in `GrpcSecurity.AUTHENTICATED_IDENTITY_BY_REMOTE_ADDR`, a
   `ConcurrentHashMap` keyed by the client's remote `InetSocketAddress`
4. Registers a connection-close listener to remove the entry when the TCP connection ends

The connection-close listener (rather than an exchange-completion listener) is used because
HTTP/2 multiplexes multiple streams over a single connection — all streams share the same
remote address, so removing the identity when one exchange completes would race with other
streams' interceptor reads.

### 2. `ElytronSecurityIdentityInterceptor`

A built-in gRPC `ServerInterceptor` that runs on every inbound call. It:

1. Looks up the identity from `AUTHENTICATED_IDENTITY_BY_REMOTE_ADDR` using the call's
   `TRANSPORT_ATTR_REMOTE_ADDR` attribute
2. Falls back to `SecurityDomain.getCurrent().getCurrentSecurityIdentity()` if the map
   lookup misses
3. Sets `GrpcSecurity.SECURITY_IDENTITY_KEY` on the gRPC `Context` so service code can
   read the identity on any thread via `Context` propagation
4. Wraps all listener callbacks (`onMessage`, `onHalfClose`, `onCancel`, `onComplete`,
   `onReady`) with `SecurityIdentity.runAs()` so that:
   - `SecurityDomain.getCurrent().getCurrentSecurityIdentity()` returns the identity on
     gRPC executor threads
   - `@RolesAllowed` CDI interceptors work transparently
   - EJBs called from gRPC services see the correct security context

For unauthenticated requests where no identity is found, the interceptor passes through
without wrapping — service code will see `null` from `SECURITY_IDENTITY_KEY.get()` and
no `SecurityIdentity` on the thread.

---

## Reading the Security Identity

### Option A: gRPC Context key (recommended for gRPC service methods)

```java
import org.wildfly.extension.grpc.GrpcSecurity;
import org.wildfly.security.auth.server.SecurityIdentity;

// Inside a gRPC service method or CDI bean called from it:
SecurityIdentity identity = GrpcSecurity.SECURITY_IDENTITY_KEY.get();
if (identity != null && !identity.isAnonymous()) {
    String caller = identity.getPrincipal().getName();
}
```

`SECURITY_IDENTITY_KEY` is set to the authenticated identity for authenticated callers,
or may be `null` if no identity could be determined. Use a null check followed by
`identity.isAnonymous()` to distinguish authenticated from anonymous callers.
The key propagates across gRPC's async thread boundaries via `io.grpc.Context`.

### Option B: Elytron thread-local (works anywhere in the call stack)

```java
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityIdentity;

SecurityDomain domain = SecurityDomain.getCurrent();
SecurityIdentity identity = domain != null ? domain.getCurrentSecurityIdentity() : null;
```

Available because `runAs()` wraps every listener callback. This is the same mechanism
that regular servlet deployments use.

---

## `@RolesAllowed` on CDI Beans

`@RolesAllowed` works transparently on CDI beans injected into gRPC services.
No extra annotation is required:

```java
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OrderService {

    @RolesAllowed("admin")
    public void placeOrder(Order order) {
        // Only reachable when the caller has the "admin" role
    }
}
```

```java
@Dependent
public class OrderGrpcService extends OrderServiceGrpc.OrderServiceImplBase {

    @Inject
    OrderService orderService;

    @Override
    public void placeOrder(PlaceOrderRequest request, StreamObserver<PlaceOrderResponse> observer) {
        try {
            orderService.placeOrder(toOrder(request));
            observer.onNext(PlaceOrderResponse.getDefaultInstance());
            observer.onCompleted();
        } catch (SecurityException e) {
            observer.onError(Status.PERMISSION_DENIED.withDescription(e.getMessage()).asRuntimeException());
        }
    }
}
```

### How it works

The gRPC subsystem module ships `GrpcSecurityCdiExtension`, a CDI Portable Extension that
activates automatically for all CDI-enabled gRPC deployments:

- **`BeforeBeanDiscovery`**: registers `GrpcRolesAllowedInterceptor` so CDI discovers it
  from the subsystem module (not the deployment archive)
- **`ProcessAnnotatedType`**: adds the internal `@GrpcSecuredBinding` interceptor binding to
  any CDI bean class that has `@RolesAllowed` on its methods — invisibly, without user action
- **`ProcessBeanAttributes`**: vetoes WildFly's exchange-based `SecurityContext` bean
- **`AfterBeanDiscovery`**: registers `GrpcElytronSecurityContext`, a `SecurityContext`
  implementation backed by `SecurityDomain.getCurrent()` instead of the Undertow exchange,
  so role checks work on gRPC executor threads
- **`GrpcRolesAllowedInterceptor`**: fires for beans bound with `@GrpcSecuredBinding` and
  checks `@RolesAllowed` values against `SecurityDomain.getCurrent().getCurrentSecurityIdentity()`,
  throwing `SecurityException` when access is denied

The extension is loaded by Weld via `META-INF/services/jakarta.enterprise.inject.spi.Extension`
from the `org.wildfly.extension.grpc` module, which `GrpcDependencyProcessor` adds to every
gRPC-capable deployment.

### Role configuration

Roles come from the `groups` attribute in the Elytron realm, mapped via `simple-role-decoder`:

```
/subsystem=elytron/simple-role-decoder=grpc-role-decoder:add(attribute=groups)
/subsystem=elytron/security-domain=grpc-domain:add(
    ...
    realms=[{realm=grpc-realm, role-decoder=grpc-role-decoder}],
    ...)
```

Then assign roles to users:

```
/subsystem=elytron/filesystem-realm=grpc-realm:add-identity-attribute(
    identity=alice, name=groups, value=[admin, grpc-user])
```

---

## Mutual TLS Client Certificate Authentication

When mutual TLS is configured on the Undertow HTTPS listener and the WAR has a security
domain configured via `jboss-web.xml`:

1. Undertow validates the client certificate at the TLS layer (`need-client-auth=true`
   on the HTTPS listener)
2. The inner handler chain wrapper in `GrpcServletInstallProcessor` extracts the
   certificate's `X500Principal` from the SSL session via `SSLSessionInfo.getPeerCertificates()`
3. The principal is authenticated against the Elytron `SecurityDomain` via
   `ServerAuthenticationContext.setAuthenticationPrincipal()`
4. The resulting `SecurityIdentity` is stored in `AUTHENTICATED_IDENTITY_BY_REMOTE_ADDR`,
   keyed by the client's remote `InetSocketAddress`
5. `ElytronSecurityIdentityInterceptor` reads the identity from the map, sets
   `SECURITY_IDENTITY_KEY`, and wraps callbacks with `runAs()`

**Important**: the `application-security-domain` mapping for mTLS uses the `security-domain`
attribute directly (not `http-authentication-factory`) and does **not** use the `CLIENT_CERT`
HTTP authentication mechanism. The Elytron filesystem-realm cannot verify
`X509PeerCertificateChainEvidence`, so activating the `CLIENT_CERT` mechanism would cause a
403 Forbidden before the request reaches the `GrpcServlet`. Instead, the inner handler chain
wrapper authenticates the TLS peer certificate directly via
`ServerAuthenticationContext.setAuthenticationPrincipal()`.

The Elytron security domain should use an `x500-attribute-principal-decoder` to extract
the relevant field (e.g., `CN`) from the certificate subject:

```
/subsystem=elytron/x500-attribute-principal-decoder=grpc-cn-decoder:add(attribute-name=CN)
/subsystem=elytron/security-domain=grpc-mtls-domain:add(
    default-realm=grpc-mtls-realm,
    principal-decoder=grpc-cn-decoder,
    realms=[{realm=grpc-mtls-realm}],
    permission-mapper=default-permission-mapper)
```

The realm must have an identity whose name matches the decoded principal (e.g., an identity
named `"client"` for a certificate with `CN=client`).

The client certificate's CN generated by the `ssl-gen` module is `client`.

---

## Summary

| Authentication mechanism | Configuration required | Identity source in service |
|---|---|---|
| HTTP BASIC (plaintext) | `jboss-web.xml` security domain + `application-security-domain` with `http-authentication-factory` | `SECURITY_IDENTITY_KEY.get()` or `SecurityDomain.getCurrent()` |
| Mutual TLS | `jboss-web.xml` security domain + `application-security-domain` with `security-domain` + `need-client-auth` on HTTPS listener | Same |
| None (unauthenticated) | No security domain in `jboss-web.xml` | `null` from `SECURITY_IDENTITY_KEY.get()` |

All mechanisms make `@RolesAllowed` work on `@Inject`-ed CDI beans without any extra
annotation on the CDI bean or the gRPC service implementation.

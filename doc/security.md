# Security and Identity Propagation for gRPC Services

## Background

Regular WildFly servlet deployments receive security identity propagation automatically:
`ElytronContextAssociationHandler` authenticates the HTTP request, registers the deployment
ClassLoader with the Elytron `SecurityDomain`, and wraps the handler chain with
`SecurityIdentity.runAs()` so that any code executing on the request thread can call
`SecurityDomain.getCurrent().getCurrentSecurityIdentity()` and get the caller's identity.

gRPC services run in a standalone servlet container managed by the gRPC subsystem. This
container is separate from WildFly's main servlet container, so gRPC requests bypass
`ElytronContextAssociationHandler` entirely. Without additional configuration, CDI beans
called from gRPC service methods execute anonymously.

The subsystem provides a complete solution through three cooperating components.

---

## Configuration: `http-authentication-factory`

Add an optional attribute to `/subsystem=grpc` to name the Elytron HTTP authentication
factory to use for authenticating gRPC callers:

```
/subsystem=grpc:write-attribute(name=http-authentication-factory, value=my-http-auth-factory)
```

The attribute uses the same name and capability (`org.wildfly.security.http-authentication-factory`)
as Undertow's `application-security-domain` resource, so the same factory that secures
a WAR's HTTP traffic can also secure gRPC calls.

A typical Elytron setup for BASIC authentication over gRPC:

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

# Wire into the gRPC subsystem
/subsystem=grpc:write-attribute(name=http-authentication-factory, value=grpc-http-auth)

# Reload
:reload
```

For mutual TLS, no BASIC auth factory is needed — client certificate authentication is
handled automatically from the TLS session (see below).

---

## How Identity Is Propagated

When `http-authentication-factory` is configured, three things happen:

### 1. ClassLoader Registration

At subsystem start, the subsystem's own ClassLoader is registered with the factory's
`SecurityDomain`:

```
factory.getSecurityDomain().registerWithClassLoader(GrpcUndertowService.class.getClassLoader())
```

This makes `SecurityDomain.getCurrent()` return the domain on any thread whose context
ClassLoader is the subsystem ClassLoader.

### 2. ClassLoader Switching Interceptor

A built-in gRPC interceptor (`SubsystemClassLoaderInterceptor`) is prepended to every service's
interceptor chain. It sets the thread's context ClassLoader to the subsystem ClassLoader
for every listener callback (`onMessage`, `onHalfClose`, etc.), ensuring `SecurityDomain.getCurrent()`
works on the gRPC executor threads where service methods run.

### 3. Authentication and Identity Propagation

`ElytronSecurityIdentityInterceptor` runs next in the chain. It:

1. Calls `SecurityDomain.getCurrent().getCurrentSecurityIdentity()` — non-null after steps 1–2
2. If no identity is set yet, tries **HTTP BASIC** from the gRPC call metadata
   (`Authorization: Basic ...` header) via `ServerAuthenticationContext`
3. For **mutual TLS**, the client certificate subject is looked up from `PEER_PRINCIPAL_BY_ADDRESS`
   (populated by the Undertow filter when a TLS peer certificate is present) and authenticated
   via `ServerAuthenticationContext.setAuthenticationPrincipal()`
4. Wraps all listener callbacks with `effectiveIdentity.runAs()` so that:
   - `SecurityDomain.getCurrent().getCurrentSecurityIdentity()` returns the identity
   - `@RolesAllowed` interceptors work on CDI beans
   - EJBs called from gRPC services see the correct security context
5. Sets `GrpcSecurity.SECURITY_IDENTITY_KEY` on the gRPC `Context` (for code that reads the
   identity via the gRPC Context API)

For unauthenticated requests, the anonymous identity is used for `runAs()` so the security
context is always non-null on the service thread.

---

## Reading the Security Identity

### Option A: gRPC Context key (recommended for gRPC service methods)

```java
import org.wildfly.extension.grpc.GrpcSecurity;
import org.wildfly.security.auth.server.SecurityIdentity;

// Inside a gRPC service method or CDI bean called from it:
SecurityIdentity identity = GrpcSecurity.SECURITY_IDENTITY_KEY.get();
if (!identity.isAnonymous()) {
    String caller = identity.getPrincipal().getName();
}
```

`SECURITY_IDENTITY_KEY` is **always set** when `http-authentication-factory` is configured —
to the authenticated identity for authenticated callers, or to the anonymous identity for
unauthenticated ones. Use `identity.isAnonymous()` (not a null check) to distinguish them.
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

- **`ProcessAnnotatedType`**: adds the internal `@GrpcSecuredBinding` interceptor binding to
  any CDI bean class that has `@RolesAllowed` on its methods — invisibly, without user action
- **`GrpcRolesAllowedInterceptor`**: fires for those beans and checks `@RolesAllowed` values
  against `SecurityDomain.getCurrent().getCurrentSecurityIdentity()`, throwing
  `SecurityException` when access is denied
- **`GrpcElytronSecurityContext`**: replaces WildFly's exchange-based `SecurityContext` CDI bean
  with one backed by Elytron's thread-local, so standard Jakarta Security infrastructure also
  uses the correct identity for gRPC calls

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

When mutual TLS is configured on the Undertow HTTPS listener and `http-authentication-factory`
is set on the gRPC subsystem:

1. Undertow validates the client certificate at the TLS layer
2. The gRPC filter extracts the certificate's subject `X500Principal` from the SSL session
3. The principal is correlated to the gRPC call via the client's remote `InetSocketAddress`
   (the same address that gRPC's `ServerCall` exposes as `TRANSPORT_ATTR_REMOTE_ADDR`)
4. `ElytronSecurityIdentityInterceptor` authenticates the principal against the Elytron
   `SecurityDomain` via `ServerAuthenticationContext.setAuthenticationPrincipal()`

The Elytron security domain should use an `x500-attribute-principal-decoder` to extract
the relevant field (e.g., `CN`) from the certificate subject:

```
/subsystem=elytron/x500-attribute-principal-decoder=grpc-cn-decoder:add(attribute-name=CN)
/subsystem=elytron/security-domain=grpc-domain:add(
    ...
    principal-decoder=grpc-cn-decoder,
    ...)
```

The realm must have an identity whose name matches the decoded principal (e.g., an identity
named `"client"` for a certificate with `CN=client`).

The client certificate's CN generated by the `ssl-gen` module is `client`.

---

## Summary

| Authentication mechanism | Configuration required | Identity source in service |
|---|---|---|
| HTTP BASIC (plaintext) | `http-authentication-factory` on `/subsystem=grpc` | `SECURITY_IDENTITY_KEY.get()` or `SecurityDomain.getCurrent()` |
| Mutual TLS | `http-authentication-factory` + mTLS on HTTPS listener | Same |
| None (unauthenticated) | Any | anonymous identity (`isAnonymous() == true`) |

All mechanisms make `@RolesAllowed` work on `@Inject`-ed CDI beans without any extra
annotation on the CDI bean or the gRPC service implementation.

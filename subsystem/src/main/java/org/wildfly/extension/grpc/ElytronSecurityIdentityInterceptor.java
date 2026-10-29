/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.grpc;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.PrivilegedAction;

import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityIdentity;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.ForwardingServerCallListener.SimpleForwardingServerCallListener;
import io.grpc.Grpc;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

/**
 * Built-in {@link ServerInterceptor} that bridges the Elytron {@link SecurityIdentity}
 * (set by {@code ElytronContextAssociationHandler} on the Undertow request thread) into
 * the gRPC {@link Context} and wraps every listener callback with
 * {@link SecurityIdentity#runAs} so that:
 * <ul>
 * <li>{@link GrpcSecurity#SECURITY_IDENTITY_KEY} returns the authenticated identity in
 * service methods.</li>
 * <li>{@link SecurityDomain#getCurrent()}.{@link SecurityDomain#getCurrentSecurityIdentity()}
 * returns a non-null identity on gRPC executor threads, enabling Elytron-aware CDI
 * interceptors (e.g. {@code @RolesAllowed}) to work transparently.</li>
 * </ul>
 *
 * <p>
 * Authentication is performed by WildFly's {@code ElytronContextAssociationHandler} in the
 * WAR's handler chain — no custom auth logic is needed here. The WAR must configure a security
 * domain via {@code jboss-web.xml} and a matching {@code application-security-domain} mapping
 * in the Undertow subsystem.
 */
public final class ElytronSecurityIdentityInterceptor implements ServerInterceptor {

    public static final ElytronSecurityIdentityInterceptor INSTANCE = new ElytronSecurityIdentityInterceptor();

    private ElytronSecurityIdentityInterceptor() {
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        SecurityIdentity identity = null;
        final SocketAddress remoteSocketAddr = call.getAttributes().get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR);
        if (remoteSocketAddr instanceof InetSocketAddress) {
            identity = GrpcSecurity.AUTHENTICATED_IDENTITY_BY_REMOTE_ADDR.get(remoteSocketAddr);
        }

        // Fallback: SecurityDomain.getCurrent() (works when classloader is registered with domain).
        if (identity == null) {
            final SecurityDomain domain = SecurityDomain.getCurrent();
            if (domain != null) {
                identity = domain.getCurrentSecurityIdentity();
            }
        }

        if (identity == null) {
            return next.startCall(call, headers);
        }

        final SecurityIdentity effectiveIdentity = identity;

        // Propagate the identity into the gRPC Context so service code can use
        // GrpcSecurity.SECURITY_IDENTITY_KEY.get() on any thread.
        final Context ctx = Context.current()
                .withValue(GrpcSecurity.SECURITY_IDENTITY_KEY, effectiveIdentity);

        // Wrap listener callbacks with runAs() so SecurityDomain.getCurrent() returns
        // the identity on gRPC executor threads — required for @RolesAllowed CDI interceptors.
        final ServerCall.Listener<ReqT> delegate = Contexts.interceptCall(ctx, call, headers, next);
        return new SimpleForwardingServerCallListener<>(delegate) {
            @Override
            public void onMessage(ReqT message) {
                effectiveIdentity.runAs((PrivilegedAction<Void>) () -> {
                    super.onMessage(message);
                    return null;
                });
            }

            @Override
            public void onHalfClose() {
                effectiveIdentity.runAs((PrivilegedAction<Void>) () -> {
                    super.onHalfClose();
                    return null;
                });
            }

            @Override
            public void onCancel() {
                effectiveIdentity.runAs((PrivilegedAction<Void>) () -> {
                    super.onCancel();
                    return null;
                });
            }

            @Override
            public void onComplete() {
                effectiveIdentity.runAs((PrivilegedAction<Void>) () -> {
                    super.onComplete();
                    return null;
                });
            }

            @Override
            public void onReady() {
                effectiveIdentity.runAs((PrivilegedAction<Void>) () -> {
                    super.onReady();
                    return null;
                });
            }
        };
    }
}

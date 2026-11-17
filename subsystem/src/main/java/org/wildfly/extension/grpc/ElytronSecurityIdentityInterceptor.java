/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.grpc;

import java.security.PrivilegedAction;

import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityIdentity;

import io.grpc.ForwardingServerCallListener.SimpleForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

/**
 * Built-in {@link ServerInterceptor} that bridges the Elytron {@link SecurityIdentity}
 * (set by {@code ElytronContextAssociationHandler} on the Undertow request thread) to
 * gRPC executor threads by wrapping every listener callback with
 * {@link SecurityIdentity#runAs} so that:
 * <ul>
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
        final SecurityIdentity identity =
                GrpcElytronIdentityContext.claim(headers.get(GrpcElytronIdentityContext.HEADER_KEY));

        if (identity == null) {
            return next.startCall(call, headers);
        }

        // Wrap listener callbacks with runAs() so SecurityDomain.getCurrent() returns
        // the identity on gRPC executor threads — required for @RolesAllowed CDI interceptors.
        final ServerCall.Listener<ReqT> delegate = next.startCall(call, headers);
        return new SimpleForwardingServerCallListener<>(delegate) {
            @Override
            public void onMessage(ReqT message) {
                identity.runAs((PrivilegedAction<Void>) () -> {
                    super.onMessage(message);
                    return null;
                });
            }

            @Override
            public void onHalfClose() {
                identity.runAs((PrivilegedAction<Void>) () -> {
                    super.onHalfClose();
                    return null;
                });
            }

            @Override
            public void onCancel() {
                identity.runAs((PrivilegedAction<Void>) () -> {
                    super.onCancel();
                    return null;
                });
            }

            @Override
            public void onComplete() {
                identity.runAs((PrivilegedAction<Void>) () -> {
                    super.onComplete();
                    return null;
                });
            }

            @Override
            public void onReady() {
                identity.runAs((PrivilegedAction<Void>) () -> {
                    super.onReady();
                    return null;
                });
            }
        };
    }
}

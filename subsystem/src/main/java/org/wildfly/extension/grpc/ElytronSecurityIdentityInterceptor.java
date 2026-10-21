/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.grpc;

import static org.wildfly.extension.grpc._private.GrpcLogger.LOGGER;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.util.Base64;

import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityIdentity;
import org.wildfly.security.auth.server.ServerAuthenticationContext;
import org.wildfly.security.evidence.PasswordGuessEvidence;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.ForwardingServerCallListener.SimpleForwardingServerCallListener;
import io.grpc.Grpc;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

/**
 * Built-in {@link ServerInterceptor} that propagates the Elytron {@link SecurityIdentity}
 * of the caller into the gRPC {@link Context} and wraps every listener callback with
 * {@link SecurityIdentity#runAs} so that:
 * <ul>
 *   <li>{@link GrpcSecurity#SECURITY_IDENTITY_KEY} returns the authenticated identity in
 *       service methods (and CDI beans called from them).</li>
 *   <li>{@link SecurityDomain#getCurrent()}.{@link SecurityDomain#getCurrentSecurityIdentity()}
 *       returns a non-null identity on the service method thread, enabling
 *       Elytron-aware CDI interceptors (e.g. {@code @RolesAllowed}) to work transparently.</li>
 * </ul>
 *
 * <p>Two authentication sources are tried in order when no identity has been established:
 * <ol>
 *   <li><b>Mutual TLS</b> — the client certificate's subject principal extracted from the
 *       Undertow SSL session by {@link GrpcUndertowService} and stored keyed by the client's
 *       {@link InetSocketAddress}.</li>
 *   <li><b>HTTP BASIC</b> — credentials in the {@code Authorization: Basic ...} gRPC metadata
 *       header, verified via {@link ServerAuthenticationContext}.</li>
 * </ol>
 *
 * <p>For unauthenticated requests the domain's anonymous identity is used for {@code runAs()},
 * keeping the security context non-null on the service method thread while leaving
 * {@link GrpcSecurity#SECURITY_IDENTITY_KEY} set to {@code null}.
 */
final class ElytronSecurityIdentityInterceptor implements ServerInterceptor {

    static final ElytronSecurityIdentityInterceptor INSTANCE = new ElytronSecurityIdentityInterceptor();

    private static final Metadata.Key<String> AUTHORIZATION_KEY =
            Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);

    private ElytronSecurityIdentityInterceptor() {
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        final SecurityDomain domain = SecurityDomain.getCurrent();
        if (domain == null) {
            return next.startCall(call, headers);
        }

        // --- Resolve the authenticated identity ---
        SecurityIdentity identity = domain.getCurrentSecurityIdentity();

        if (identity == null || identity.isAnonymous()) {
            // 1. Try mTLS: peer principal keyed by remote address
            final SocketAddress remoteAddr = call.getAttributes().get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR);
            if (remoteAddr instanceof InetSocketAddress) {
                final Principal peerPrincipal =
                        GrpcUndertowService.PEER_PRINCIPAL_BY_ADDRESS.get(remoteAddr);
                if (peerPrincipal != null) {
                    identity = authenticateCertificate(domain, peerPrincipal);
                }
            }
        }
        if (identity == null || identity.isAnonymous()) {
            // 2. Try HTTP BASIC from gRPC call metadata
            identity = authenticateBasic(domain, headers);
        }

        final boolean authenticated = identity != null && !identity.isAnonymous();

        // Use the authenticated identity or the anonymous identity as a unified "effective" identity.
        // Anonymous keeps the security context non-null for @RolesAllowed interceptors.
        final SecurityIdentity effectiveIdentity = authenticated
                ? identity : domain.getAnonymousSecurityIdentity();

        // SECURITY_IDENTITY_KEY is always set — to the authenticated identity or to the
        // anonymous identity — so service code can use isAnonymous() consistently instead
        // of null-checking, and both SECURITY_IDENTITY_KEY.get() and
        // SecurityDomain.getCurrent().getCurrentSecurityIdentity() behave identically.
        final Context ctx = Context.current()
                .withValue(GrpcSecurity.SECURITY_IDENTITY_KEY, effectiveIdentity);

        // Build the downstream listener, then wrap each callback with runAs() so that
        // SecurityDomain.getCurrent().getCurrentSecurityIdentity() works inside service methods.
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

    private static SecurityIdentity authenticateCertificate(SecurityDomain domain,
            Principal peerPrincipal) {
        try {
            final ServerAuthenticationContext sac = domain.createNewAuthenticationContext();
            try {
                sac.setAuthenticationPrincipal(peerPrincipal);
                if (sac.authorize()) {
                    return sac.getAuthorizedIdentity();
                }
            } finally {
                sac.close();
            }
        } catch (Exception e) {
            LOGGER.debugf("gRPC certificate authentication failed: %s", e.getMessage());
        }
        return null;
    }

    private static SecurityIdentity authenticateBasic(SecurityDomain domain, Metadata headers) {
        final String authHeader = headers.get(AUTHORIZATION_KEY);
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            return null;
        }
        try {
            final String decoded = new String(
                    Base64.getDecoder().decode(authHeader.substring(6)), StandardCharsets.UTF_8);
            final int colon = decoded.indexOf(':');
            if (colon < 1) {
                return null;
            }
            final String username = decoded.substring(0, colon);
            final char[] password = decoded.substring(colon + 1).toCharArray();

            final ServerAuthenticationContext sac = domain.createNewAuthenticationContext();
            try {
                sac.setAuthenticationName(username);
                if (sac.verifyEvidence(new PasswordGuessEvidence(password)) && sac.authorize()) {
                    return sac.getAuthorizedIdentity();
                }
            } finally {
                sac.close();
            }
        } catch (Exception e) {
            LOGGER.debugf("gRPC BASIC authentication failed: %s", e.getMessage());
        }
        return null;
    }
}

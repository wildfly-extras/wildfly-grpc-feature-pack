/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.grpc;

import static org.wildfly.extension.grpc._private.GrpcLogger.LOGGER;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityIdentity;
import org.wildfly.security.auth.server.ServerAuthenticationContext;
import org.wildfly.security.evidence.PasswordGuessEvidence;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

/**
 * Built-in {@link ServerInterceptor} that propagates the Elytron {@link SecurityIdentity}
 * of the caller into the gRPC {@link Context} via {@link GrpcSecurity#SECURITY_IDENTITY_KEY}.
 *
 * <p>When {@code http-authentication-factory} is configured on the gRPC subsystem, the
 * {@code SubsystemClassLoaderInterceptor} (which wraps this one) switches the thread's
 * context ClassLoader to the subsystem ClassLoader before this interceptor runs, making
 * {@link SecurityDomain#getCurrent()} return the configured domain.
 *
 * <p>Two identity sources are tried in order:
 * <ol>
 *   <li><b>Undertow security context</b> — if Undertow has already authenticated the caller
 *       (e.g. via host-level authentication), {@link SecurityDomain#getCurrent()} returns a
 *       domain and {@link SecurityDomain#getCurrentSecurityIdentity()} returns the identity.
 *   <li><b>HTTP BASIC from gRPC metadata</b> — if the call carries an
 *       {@code Authorization: Basic <credentials>} header and a security domain is available,
 *       the credentials are verified and the resulting identity is used.
 * </ol>
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
        if (domain != null) {
            SecurityIdentity identity = domain.getCurrentSecurityIdentity();

            // If no identity has been established by Undertow, try HTTP BASIC from gRPC metadata.
            if (identity == null || identity.isAnonymous()) {
                identity = authenticateBasic(domain, headers);
            }

            if (identity != null && !identity.isAnonymous()) {
                final Context ctx = Context.current()
                        .withValue(GrpcSecurity.SECURITY_IDENTITY_KEY, identity);
                return Contexts.interceptCall(ctx, call, headers, next);
            }
        }
        return next.startCall(call, headers);
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

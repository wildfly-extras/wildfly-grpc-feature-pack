/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.grpc;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

import org.wildfly.security.auth.server.SecurityIdentity;

import io.grpc.Metadata;

/**
 * Per-request handoff between the servlet security context and gRPC.
 *
 * <p>The servlet filter creates a token for one HTTP request and injects it into the
 * request passed to {@code GrpcServlet}. The gRPC interceptor consumes the token once.
 * The token is deliberately scoped to a request, rather than an HTTP/2 connection: a
 * single connection can carry streams authenticated as different identities.
 */
final class GrpcElytronIdentityContext {

    static final String HEADER_NAME = "x-wildfly-elytron-context-token";
    static final Metadata.Key<String> HEADER_KEY =
            Metadata.Key.of(HEADER_NAME, Metadata.ASCII_STRING_MARSHALLER);

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final ConcurrentHashMap<String, SecurityIdentity> IDENTITIES = new ConcurrentHashMap<>();

    private GrpcElytronIdentityContext() {
    }

    static String register(final SecurityIdentity identity) {
        final byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        final String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        IDENTITIES.put(token, identity);
        return token;
    }

    static SecurityIdentity claim(final String token) {
        return token == null ? null : IDENTITIES.remove(token);
    }

    static void discard(final String token) {
        IDENTITIES.remove(token);
    }
}

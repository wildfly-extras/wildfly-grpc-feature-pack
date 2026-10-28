/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.grpc;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

import org.wildfly.security.auth.server.SecurityIdentity;

import io.grpc.Context;

/**
 * Public API holder for gRPC security integration.
 * <p>
 * The built-in {@code ElytronSecurityIdentityInterceptor} populates
 * {@link #SECURITY_IDENTITY_KEY} on every inbound gRPC call. Service
 * implementations can read the caller's identity as:
 *
 * <pre>
 * SecurityIdentity id = GrpcSecurity.SECURITY_IDENTITY_KEY.get();
 * </pre>
 */
public final class GrpcSecurity {

    /**
     * gRPC {@link Context} key carrying the Elytron {@link SecurityIdentity} of the caller.
     * Set by the subsystem's built-in interceptor before any user-defined interceptors run.
     * Returns {@code null} if the identity could not be determined (e.g. unauthenticated and
     * the anonymous identity is not available).
     */
    public static final Context.Key<SecurityIdentity> SECURITY_IDENTITY_KEY = Context.key("wildfly.grpc.security-identity");

    /**
     * Bridges the authenticated {@link SecurityIdentity} from the Undertow handler chain
     * (where {@code ElytronContextAssociationHandler} runs) to the gRPC executor thread
     * (where {@code ElytronSecurityIdentityInterceptor.interceptCall()} runs).
     * <p>
     * Keyed by the client's remote {@link InetSocketAddress}. Entries are added per-exchange
     * and removed when the underlying connection closes, ensuring the identity remains
     * available for all multiplexed HTTP/2 streams on the same connection.
     */
    public static final ConcurrentHashMap<InetSocketAddress, SecurityIdentity>
            AUTHENTICATED_IDENTITY_BY_REMOTE_ADDR = new ConcurrentHashMap<>();

    private GrpcSecurity() {
    }
}

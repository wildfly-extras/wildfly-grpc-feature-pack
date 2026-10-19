/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.grpc;

import org.wildfly.security.auth.server.SecurityIdentity;

import io.grpc.Context;

/**
 * Public API holder for gRPC security integration.
 * <p>
 * The built-in {@code ElytronSecurityIdentityInterceptor} populates
 * {@link #SECURITY_IDENTITY_KEY} on every inbound gRPC call. Service
 * implementations can read the caller's identity as:
 * <pre>
 *     SecurityIdentity id = GrpcSecurity.SECURITY_IDENTITY_KEY.get();
 * </pre>
 */
public final class GrpcSecurity {

    /**
     * gRPC {@link Context} key carrying the Elytron {@link SecurityIdentity} of the caller.
     * Set by the subsystem's built-in interceptor before any user-defined interceptors run.
     * Returns {@code null} if the identity could not be determined (e.g. unauthenticated and
     * the anonymous identity is not available).
     */
    public static final Context.Key<SecurityIdentity> SECURITY_IDENTITY_KEY =
            Context.key("wildfly.grpc.security-identity");

    private GrpcSecurity() {
    }
}

/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.grpc.security;

import java.util.Arrays;

import jakarta.annotation.Priority;
import jakarta.annotation.security.RolesAllowed;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * CDI interceptor that enforces {@link RolesAllowed} for beans in gRPC service deployments.
 *
 * <p>Bound via {@link GrpcSecuredBinding}, which {@link GrpcSecurityCdiExtension} adds
 * automatically to any annotated type whose methods carry {@link RolesAllowed}. Reads the
 * identity from {@link GrpcElytronSecurityContext}, which delegates to
 * {@link org.wildfly.security.auth.server.SecurityDomain#getCurrent()} — populated on gRPC
 * executor threads by {@code ElytronSecurityIdentityInterceptor} via
 * {@code SecurityIdentity.runAs()}.
 */
@GrpcSecuredBinding
@Interceptor
@Priority(Interceptor.Priority.APPLICATION + 10)
public class GrpcRolesAllowedInterceptor {

    @AroundInvoke
    public Object enforceRolesAllowed(InvocationContext ctx) throws Exception {
        RolesAllowed rolesAllowed = ctx.getMethod().getAnnotation(RolesAllowed.class);
        if (rolesAllowed == null) {
            rolesAllowed = ctx.getTarget().getClass().getAnnotation(RolesAllowed.class);
        }
        if (rolesAllowed != null && rolesAllowed.value().length > 0) {
            final GrpcElytronSecurityContext sc = new GrpcElytronSecurityContext();
            final boolean hasRole = Arrays.stream(rolesAllowed.value()).anyMatch(sc::isCallerInRole);
            if (!hasRole) {
                throw new SecurityException(
                        "Access denied: requires one of " + Arrays.toString(rolesAllowed.value()));
            }
        }
        return ctx.proceed();
    }
}

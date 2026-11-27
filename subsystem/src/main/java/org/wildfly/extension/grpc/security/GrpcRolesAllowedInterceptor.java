/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.grpc.security;

import java.util.Arrays;

import static org.wildfly.extension.grpc._private.GrpcLogger.LOGGER;

import jakarta.annotation.Priority;
import jakarta.annotation.security.RolesAllowed;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityIdentity;

/**
 * CDI interceptor that enforces {@link RolesAllowed} for beans in gRPC service deployments.
 *
 * <p>Bound via {@link GrpcSecuredBinding}, which {@link GrpcSecurityCdiExtension} adds
 * automatically to any annotated type whose methods carry {@link RolesAllowed}. Reads the
 * identity directly from {@link SecurityDomain#getCurrent()}, populated on gRPC executor
 * threads by {@code ElytronSecurityIdentityInterceptor} via
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
            final SecurityDomain domain = SecurityDomain.getCurrent();
            final SecurityIdentity identity = domain == null ? null : domain.getCurrentSecurityIdentity();
            final boolean hasRole = identity != null && !identity.isAnonymous()
                    && Arrays.stream(rolesAllowed.value()).anyMatch(identity.getRoles()::contains);
            if (!hasRole) {
                throw LOGGER.accessDenied(Arrays.toString(rolesAllowed.value()));
            }
        }
        return ctx.proceed();
    }
}

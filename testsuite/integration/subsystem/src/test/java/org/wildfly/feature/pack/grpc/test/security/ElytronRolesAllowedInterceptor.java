/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.feature.pack.grpc.test.security;

import java.util.Arrays;
import java.util.Collection;

import jakarta.annotation.Priority;
import jakarta.annotation.security.RolesAllowed;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityIdentity;

/**
 * CDI interceptor that enforces {@link RolesAllowed} using Elytron's
 * {@link SecurityDomain#getCurrent()}.
 *
 * <p>WildFly's built-in {@link RolesAllowed} CDI interceptor uses the Jakarta EE
 * request-scoped {@code SecurityContext}, which is not populated for gRPC services
 * (gRPC requests bypass the normal Undertow security handler chain). This interceptor
 * reads the identity directly from Elytron's thread-local, which IS populated because
 * {@code ElytronSecurityIdentityInterceptor} wraps every gRPC listener callback with
 * {@link SecurityIdentity#runAs} — making {@code SecurityDomain.getCurrent().getCurrentSecurityIdentity()}
 * return the authenticated (or anonymous) identity for the duration of the service call.
 *
 * <p>The interceptor is bound via {@link ElytronRolesAllowedBinding} so that it fires
 * for classes or methods carrying that annotation. {@link RolesAllowed} remains on the
 * individual methods to document which roles are required.
 */
@ElytronRolesAllowedBinding
@Interceptor
@Priority(jakarta.interceptor.Interceptor.Priority.APPLICATION + 10)
public class ElytronRolesAllowedInterceptor {

    @AroundInvoke
    public Object enforceRolesAllowed(InvocationContext ctx) throws Exception {
        final RolesAllowed rolesAllowed = ctx.getMethod().getAnnotation(RolesAllowed.class);
        if (rolesAllowed != null && rolesAllowed.value().length > 0) {
            checkAccess(rolesAllowed.value());
        }
        return ctx.proceed();
    }

    private static void checkAccess(String[] requiredRoles) {
        final SecurityDomain domain = SecurityDomain.getCurrent();
        if (domain == null) {
            throw new SecurityException(
                    "No Elytron SecurityDomain active — ensure the gRPC subsystem "
                    + "has http-authentication-factory configured");
        }
        final SecurityIdentity identity = domain.getCurrentSecurityIdentity();
        if (identity == null || identity.isAnonymous()) {
            throw new SecurityException("Access denied: unauthenticated caller");
        }
        // Roles are mapped from the realm's "groups" attribute via the simple-role-decoder
        final Collection<String> groups = identity.getAttributes().get("groups");
        final boolean hasRole = groups != null
                && Arrays.stream(requiredRoles).anyMatch(groups::contains);
        if (!hasRole) {
            throw new SecurityException(
                    "Access denied for '" + identity.getPrincipal().getName()
                    + "': requires one of " + Arrays.toString(requiredRoles));
        }
    }
}

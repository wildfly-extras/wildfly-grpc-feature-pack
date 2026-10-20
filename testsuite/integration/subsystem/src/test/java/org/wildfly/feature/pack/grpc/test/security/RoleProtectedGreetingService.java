/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.feature.pack.grpc.test.security;

import jakarta.enterprise.context.ApplicationScoped;

import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityIdentity;

/**
 * CDI bean whose {@code greet()} method enforces role-based access control
 * by reading the Elytron {@link SecurityIdentity} directly from
 * {@link SecurityDomain#getCurrent()}.
 *
 * <p>This works because the gRPC subsystem's {@code ElytronSecurityIdentityInterceptor}
 * wraps every service-method callback with {@link SecurityIdentity#runAs}, making the
 * current identity ambient on the calling thread — exactly as
 * {@code ElytronContextAssociationHandler} does for regular servlet deployments.
 *
 * <p>In practice this is equivalent to annotating the method with
 * {@code @RolesAllowed("grpc-user")}: the CDI security interceptor that enforces
 * {@code @RolesAllowed} uses the same {@code SecurityDomain.getCurrent()} call internally.
 */
@ApplicationScoped
public class RoleProtectedGreetingService {

    static final String REQUIRED_ROLE = "grpc-user";

    /**
     * Returns a greeting if the caller's identity carries the {@value #REQUIRED_ROLE} role,
     * throws {@link SecurityException} otherwise.
     */
    public String greet(String name) {
        final SecurityDomain domain = SecurityDomain.getCurrent();
        if (domain == null) {
            throw new SecurityException("No Elytron security domain active on the current thread");
        }
        final SecurityIdentity identity = domain.getCurrentSecurityIdentity();
        if (!hasRole(identity, REQUIRED_ROLE)) {
            final String caller = identity != null ? identity.getPrincipal().getName() : "null";
            throw new SecurityException(
                    "Access denied for '" + caller + "': requires role '" + REQUIRED_ROLE + "'");
        }
        return "Secured Hello " + name;
    }

    private static boolean hasRole(SecurityIdentity identity, String role) {
        if (identity == null || identity.isAnonymous()) {
            return false;
        }
        // Roles are stored as the "groups" attribute in filesystem-realm entries.
        // SecurityIdentity.getAttributes() is the programmatic equivalent of @RolesAllowed.
        return identity.getAttributes().get("groups") != null
                && identity.getAttributes().get("groups").contains(role);
    }
}

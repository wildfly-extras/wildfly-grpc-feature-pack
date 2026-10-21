/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.grpc.security;

import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import jakarta.security.enterprise.AuthenticationStatus;
import jakarta.security.enterprise.SecurityContext;
import jakarta.security.enterprise.authentication.mechanism.http.AuthenticationParameters;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityIdentity;

/**
 * Elytron-backed implementation of Jakarta Security's {@link SecurityContext}.
 *
 * <p>WildFly's built-in {@link SecurityContext} reads from the Undertow exchange-based
 * security context (set by {@code ElytronContextAssociationHandler}), which is not
 * populated for gRPC requests that bypass the normal Undertow deployment handler chain.
 *
 * <p>This implementation reads directly from
 * {@link SecurityDomain#getCurrent()}.{@link SecurityDomain#getCurrentSecurityIdentity()},
 * which IS populated for gRPC service methods because {@code ElytronSecurityIdentityInterceptor}
 * wraps every listener callback with {@link SecurityIdentity#runAs}.
 *
 * <p>This enables standard Jakarta Security annotations such as {@code @RolesAllowed}
 * to work on CDI beans injected into gRPC services without any additional
 * configuration by the application developer.
 */
public class GrpcElytronSecurityContext implements SecurityContext {

    @Override
    public Principal getCallerPrincipal() {
        final SecurityIdentity identity = getIdentity();
        return identity != null && !identity.isAnonymous() ? identity.getPrincipal() : null;
    }

    @Override
    public <T extends Principal> Set<T> getPrincipalsByType(Class<T> pType) {
        final Principal principal = getCallerPrincipal();
        if (principal != null && pType.isInstance(principal)) {
            final Set<T> result = new HashSet<>();
            result.add(pType.cast(principal));
            return Collections.unmodifiableSet(result);
        }
        return Collections.emptySet();
    }

    @Override
    public boolean isCallerInRole(String role) {
        final SecurityIdentity identity = getIdentity();
        if (identity == null || identity.isAnonymous()) {
            return false;
        }
        // Roles are mapped from the "groups" attribute by a role-decoder in the security domain
        final Collection<String> groups = identity.getAttributes().get("groups");
        return groups != null && groups.contains(role);
    }

    @Override
    public Set<String> getAllDeclaredCallerRoles() {
        final SecurityIdentity identity = getIdentity();
        if (identity == null || identity.isAnonymous()) {
            return Collections.emptySet();
        }
        final Collection<String> groups = identity.getAttributes().get("groups");
        return groups != null ? new HashSet<>(groups) : Collections.emptySet();
    }

    @Override
    public boolean hasAccessToWebResource(String resource, String... methods) {
        return false; // Not applicable for gRPC
    }

    @Override
    public AuthenticationStatus authenticate(HttpServletRequest request,
            HttpServletResponse response, AuthenticationParameters parameters) {
        return AuthenticationStatus.NOT_DONE; // Not applicable for gRPC
    }

    private static SecurityIdentity getIdentity() {
        final SecurityDomain domain = SecurityDomain.getCurrent();
        return domain != null ? domain.getCurrentSecurityIdentity() : null;
    }
}

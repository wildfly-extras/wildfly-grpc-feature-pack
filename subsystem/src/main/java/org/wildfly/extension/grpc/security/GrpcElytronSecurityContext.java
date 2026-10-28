/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.grpc.security;

import java.security.Principal;
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
 * security context, which is not available on gRPC executor threads. This implementation
 * reads from {@link SecurityDomain#getCurrent()}, which is populated by
 * {@code ElytronSecurityIdentityInterceptor} wrapping every listener callback with
 * {@link SecurityIdentity#runAs}.
 *
 * <p>Registered by {@link GrpcSecurityCdiExtension}, which vetoes WildFly's default
 * {@link SecurityContext} bean and substitutes this one for CDI deployments that contain
 * gRPC services.
 */
public class GrpcElytronSecurityContext implements SecurityContext {

    @Override
    public Principal getCallerPrincipal() {
        final SecurityIdentity identity = getIdentity();
        return (identity != null && !identity.isAnonymous()) ? identity.getPrincipal() : null;
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
        return identity.getRoles().contains(role);
    }

    @Override
    public Set<String> getAllDeclaredCallerRoles() {
        final SecurityIdentity identity = getIdentity();
        if (identity == null || identity.isAnonymous()) {
            return Collections.emptySet();
        }
        final Set<String> result = new HashSet<>();
        identity.getRoles().forEach(result::add);
        return Collections.unmodifiableSet(result);
    }

    @Override
    public boolean hasAccessToWebResource(String resource, String... methods) {
        return false;
    }

    @Override
    public AuthenticationStatus authenticate(HttpServletRequest request,
            HttpServletResponse response, AuthenticationParameters parameters) {
        return AuthenticationStatus.NOT_DONE;
    }

    private static SecurityIdentity getIdentity() {
        final SecurityDomain domain = SecurityDomain.getCurrent();
        return domain != null ? domain.getCurrentSecurityIdentity() : null;
    }
}

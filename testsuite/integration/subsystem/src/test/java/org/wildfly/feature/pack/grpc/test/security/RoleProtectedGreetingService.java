/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.feature.pack.grpc.test.security;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * CDI bean whose {@code greet()} method is protected by {@code @RolesAllowed("grpc-user")}.
 *
 * <p>The feature pack's internal Elytron CDI extension automatically binds its role
 * interceptor to this bean, which checks the Elytron identity exposed via
 * {@code SecurityDomain.getCurrent().getCurrentSecurityIdentity()}. That identity is
 * populated on gRPC executor threads by {@code ElytronSecurityIdentityInterceptor} using
 * {@code SecurityIdentity.runAs()}.
 */
@ApplicationScoped
public class RoleProtectedGreetingService {

    @RolesAllowed("grpc-user")
    public String greet(String name) {
        return "Secured Hello " + name;
    }
}

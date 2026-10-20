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
 * <p>{@link ElytronRolesAllowedBinding} activates {@link ElytronRolesAllowedInterceptor},
 * which reads the caller's {@link org.wildfly.security.auth.server.SecurityIdentity} from
 * {@link org.wildfly.security.auth.server.SecurityDomain#getCurrent()} and enforces
 * {@code @RolesAllowed} using Elytron's role model.
 *
 * <p>This works because the gRPC subsystem's {@code ElytronSecurityIdentityInterceptor}
 * wraps every service-method callback with {@link org.wildfly.security.auth.server.SecurityIdentity#runAs},
 * making the identity ambient on the calling thread — exactly as
 * {@code ElytronContextAssociationHandler} does for regular servlet deployments.
 */
@ApplicationScoped
@ElytronRolesAllowedBinding
public class RoleProtectedGreetingService {

    @RolesAllowed("grpc-user")
    public String greet(String name) {
        return "Secured Hello " + name;
    }
}

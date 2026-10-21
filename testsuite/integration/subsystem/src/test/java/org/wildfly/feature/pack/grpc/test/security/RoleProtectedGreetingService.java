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
 * <p>The gRPC subsystem provides {@code GrpcSecurityCdiExtension}, a CDI Portable Extension
 * that replaces WildFly's exchange-based {@code SecurityContext} bean with an Elytron-backed
 * implementation reading from {@code SecurityDomain.getCurrent()}. Combined with the
 * {@code ElytronSecurityIdentityInterceptor} wrapping every listener callback with
 * {@code SecurityIdentity.runAs()}, standard {@code @RolesAllowed} enforcement works
 * transparently for CDI beans injected into gRPC services — exactly as it does for
 * regular servlet deployments via {@code ElytronContextAssociationHandler}.
 */
@ApplicationScoped
public class RoleProtectedGreetingService {

    @RolesAllowed("grpc-user")
    public String greet(String name) {
        return "Secured Hello " + name;
    }
}

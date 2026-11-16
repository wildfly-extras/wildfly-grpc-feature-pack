/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.grpc.security;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;

/**
 * CDI Portable Extension that enables transparent {@link RolesAllowed} enforcement
 * for CDI beans in gRPC service deployments.
 *
 * <ol>
 *   <li>Registers {@link GrpcRolesAllowedInterceptor} so CDI discovers it from the
 *       subsystem module (not the deployment archive).</li>
 *   <li>Automatically adds {@link GrpcSecuredBinding} to any annotated type whose methods
 *       carry {@link RolesAllowed}, so application code only needs {@code @RolesAllowed}.</li>
 * </ol>
 *
 * <p>Loaded by Weld via {@code META-INF/services/jakarta.enterprise.inject.spi.Extension}
 * from the {@code org.wildfly.extension.grpc} module, which
 * {@code GrpcDependencyProcessor} adds to every gRPC-capable deployment.
 */
public class GrpcSecurityCdiExtension implements Extension {

    void beforeBeanDiscovery(@jakarta.enterprise.event.Observes BeforeBeanDiscovery bbd,
            BeanManager bm) {
        bbd.addAnnotatedType(
                bm.createAnnotatedType(GrpcRolesAllowedInterceptor.class),
                GrpcRolesAllowedInterceptor.class.getName());
    }

    <T> void addInterceptorBindingToRolesAllowedTypes(
            @jakarta.enterprise.event.Observes ProcessAnnotatedType<T> event) {
        final Class<T> javaClass = event.getAnnotatedType().getJavaClass();
        if (GrpcRolesAllowedInterceptor.class.equals(javaClass)
                || GrpcSecuredBinding.class.equals(javaClass)) {
            return;
        }
        final boolean hasRolesAllowed = event.getAnnotatedType().isAnnotationPresent(RolesAllowed.class)
                || event.getAnnotatedType().getMethods().stream()
                        .anyMatch(m -> m.isAnnotationPresent(RolesAllowed.class));
        if (hasRolesAllowed) {
            event.configureAnnotatedType().add(GrpcSecuredBinding.Literal.INSTANCE);
        }
    }

}

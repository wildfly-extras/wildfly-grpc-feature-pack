/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.grpc.security;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import jakarta.enterprise.inject.spi.ProcessBeanAttributes;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.security.enterprise.SecurityContext;

/**
 * CDI Portable Extension that enables transparent {@link RolesAllowed} enforcement
 * for CDI beans in gRPC service deployments.
 *
 * <ol>
 *   <li>Registers {@link GrpcRolesAllowedInterceptor} so CDI discovers it from the
 *       subsystem module (not the deployment archive).</li>
 *   <li>Automatically adds {@link GrpcSecuredBinding} to any annotated type whose methods
 *       carry {@link RolesAllowed}, so application code only needs {@code @RolesAllowed}.</li>
 *   <li>Vetoes WildFly's exchange-based {@link SecurityContext} bean and registers
 *       {@link GrpcElytronSecurityContext} instead, so that role checks read from
 *       {@code SecurityDomain.getCurrent()} — populated on gRPC executor threads by
 *       {@code ElytronSecurityIdentityInterceptor} via {@code SecurityIdentity.runAs()}.</li>
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

    <T> void vetDefaultSecurityContext(
            @jakarta.enterprise.event.Observes ProcessBeanAttributes<T> event) {
        final Set<Type> types = event.getBeanAttributes().getTypes();
        if (types.contains(SecurityContext.class)
                && event.getBeanAttributes().getQualifiers().stream()
                        .anyMatch(q -> q.annotationType() == Default.class)) {
            final String desc = event.getBeanAttributes().toString();
            if (!desc.contains(GrpcElytronSecurityContext.class.getName())) {
                event.veto();
            }
        }
    }

    void registerElytronSecurityContext(
            @jakarta.enterprise.event.Observes AfterBeanDiscovery abd, BeanManager bm) {
        abd.addBean(new Bean<SecurityContext>() {
            @Override
            public Class<?> getBeanClass() {
                return GrpcElytronSecurityContext.class;
            }

            @Override
            public Set<Type> getTypes() {
                return Set.of(SecurityContext.class, Object.class);
            }

            @Override
            public Set<Annotation> getQualifiers() {
                return Set.of(new AnnotationLiteral<Default>() {}, new AnnotationLiteral<Any>() {});
            }

            @Override
            public Class<? extends Annotation> getScope() {
                return ApplicationScoped.class;
            }

            @Override
            public String getName() {
                return null;
            }

            @Override
            public Set<Class<? extends Annotation>> getStereotypes() {
                return Collections.emptySet();
            }

            @Override
            public boolean isAlternative() {
                return false;
            }

            @Override
            public SecurityContext create(CreationalContext<SecurityContext> ctx) {
                return new GrpcElytronSecurityContext();
            }

            @Override
            public void destroy(SecurityContext i, CreationalContext<SecurityContext> ctx) {
                ctx.release();
            }

            @Override
            public Set<InjectionPoint> getInjectionPoints() {
                return Collections.emptySet();
            }

            @Override
            public String toString() {
                return GrpcElytronSecurityContext.class.getName() + " bean";
            }
        });
    }
}

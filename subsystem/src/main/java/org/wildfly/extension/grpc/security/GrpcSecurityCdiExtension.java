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
 * CDI Portable Extension that provides transparent {@link RolesAllowed} enforcement
 * for CDI beans in gRPC service deployments.
 *
 * <p>This extension does three things:
 * <ol>
 *   <li><b>Registers {@link GrpcRolesAllowedInterceptor}</b> — a CDI interceptor that reads
 *       the Elytron {@link org.wildfly.security.auth.server.SecurityIdentity} via
 *       {@code SecurityDomain.getCurrent()} (made available by
 *       {@code ElytronSecurityIdentityInterceptor}'s {@code runAs()} wrapping) and enforces
 *       the roles declared by {@link RolesAllowed}.</li>
 *   <li><b>Automatically adds {@link GrpcSecuredBinding}</b> to any annotated type whose
 *       methods carry {@link RolesAllowed}, so application code only needs
 *       {@code @RolesAllowed} — no extra annotation is required.</li>
 *   <li><b>Replaces WildFly's exchange-based {@link SecurityContext}</b> bean with
 *       {@link GrpcElytronSecurityContext}, so that standard WildFly / Jakarta Security
 *       infrastructure that injects {@link SecurityContext} also uses Elytron's thread-local.</li>
 * </ol>
 *
 * <p>This extension is loaded by Weld via
 * {@code META-INF/services/jakarta.enterprise.inject.spi.Extension}
 * when the gRPC subsystem module is on the deployment's classpath (added automatically
 * by {@code GrpcDependencyProcessor} for all CDI-enabled gRPC deployments).
 */
public class GrpcSecurityCdiExtension implements Extension {

    // -------------------------------------------------------------------------
    // 1. Register the interceptor class and binding
    // -------------------------------------------------------------------------

    void beforeBeanDiscovery(@jakarta.enterprise.event.Observes BeforeBeanDiscovery bbd,
            BeanManager bm) {
        // Register the interceptor class so CDI discovers it even though it's in a
        // system module (not in the deployment's WEB-INF/lib).
        bbd.addAnnotatedType(
                bm.createAnnotatedType(GrpcRolesAllowedInterceptor.class),
                GrpcRolesAllowedInterceptor.class.getName());
    }

    // -------------------------------------------------------------------------
    // 2. Automatically apply @GrpcSecuredBinding to types with @RolesAllowed
    // -------------------------------------------------------------------------

    <T> void addInterceptorBindingToRolesAllowedTypes(
            @jakarta.enterprise.event.Observes ProcessAnnotatedType<T> event) {
        final Class<T> javaClass = event.getAnnotatedType().getJavaClass();
        // Skip our own classes to avoid infinite loops
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

    // -------------------------------------------------------------------------
    // 3. Veto WildFly's SecurityContext and register our Elytron-backed version
    // -------------------------------------------------------------------------

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

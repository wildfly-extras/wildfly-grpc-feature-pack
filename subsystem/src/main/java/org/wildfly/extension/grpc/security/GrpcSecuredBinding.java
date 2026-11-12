/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.grpc.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.interceptor.InterceptorBinding;

/**
 * CDI interceptor binding that activates {@link GrpcRolesAllowedInterceptor}.
 * Added automatically by {@link GrpcSecurityCdiExtension} to any annotated type
 * whose methods carry {@link jakarta.annotation.security.RolesAllowed}.
 */
@InterceptorBinding
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface GrpcSecuredBinding {

    class Literal extends AnnotationLiteral<GrpcSecuredBinding> implements GrpcSecuredBinding {
        public static final Literal INSTANCE = new Literal();
        private static final long serialVersionUID = 1L;
    }
}

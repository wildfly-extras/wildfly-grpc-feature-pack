/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.feature.pack.grpc.test.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.interceptor.InterceptorBinding;

/**
 * CDI {@link InterceptorBinding} that activates {@link ElytronRolesAllowedInterceptor}.
 * Place this annotation on a CDI bean class to enable Elytron-based {@code @RolesAllowed}
 * enforcement for all methods of that class.
 */
@InterceptorBinding
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface ElytronRolesAllowedBinding {
}

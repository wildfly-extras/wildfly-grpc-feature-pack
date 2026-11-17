/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.feature.pack.grpc.test.security;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import messages.HelloReply;
import messages.HelloRequest;

import org.wildfly.feature.pack.grpc.test.helloworld.GreeterGrpc;

/**
 * gRPC service implementation that delegates to {@link RoleProtectedGreetingService}.
 */
@Dependent
public class RolesAllowedGreeterServiceImpl extends GreeterGrpc.GreeterImplBase {

    @Inject
    RoleProtectedGreetingService greetingService;

    @Override
    public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
        try {
            assertSubsystemClassesAreHidden();
            final String message = greetingService.greet(request.getName());
            responseObserver.onNext(HelloReply.newBuilder().setMessage(message).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.PERMISSION_DENIED
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    /**
     * The CDI extension is registered by the subsystem, not discovered from a module dependency
     * visible to the application. Keep this check free of compile-time references to those types.
     */
    private void assertSubsystemClassesAreHidden() throws ClassNotFoundException {
        final ClassLoader applicationClassLoader = getClass().getClassLoader();
        assertClassIsHidden(applicationClassLoader, "org.wildfly.extension.grpc.ElytronSecurityIdentityInterceptor");
        assertClassIsHidden(applicationClassLoader, "org.wildfly.extension.grpc.security.GrpcSecurityCdiExtension");
    }

    private static void assertClassIsHidden(final ClassLoader classLoader, final String className)
            throws ClassNotFoundException {
        try {
            classLoader.loadClass(className);
            throw new IllegalStateException("Subsystem implementation class leaked to deployment: " + className);
        } catch (ClassNotFoundException expected) {
            // Expected: application modules do not depend on the gRPC subsystem module.
        }
    }
}

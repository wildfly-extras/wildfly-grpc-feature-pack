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
 * The role check in the CDI bean uses {@link org.wildfly.extension.grpc.GrpcSecurity#SECURITY_IDENTITY_KEY}
 * which is populated by the subsystem's built-in {@code ElytronSecurityIdentityInterceptor}.
 */
@Dependent
public class RolesAllowedGreeterServiceImpl extends GreeterGrpc.GreeterImplBase {

    @Inject
    RoleProtectedGreetingService greetingService;

    @Override
    public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
        try {
            final String message = greetingService.greet(request.getName());
            responseObserver.onNext(HelloReply.newBuilder().setMessage(message).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.PERMISSION_DENIED
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }
}

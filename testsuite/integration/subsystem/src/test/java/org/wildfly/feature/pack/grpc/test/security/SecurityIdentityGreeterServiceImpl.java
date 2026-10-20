/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.feature.pack.grpc.test.security;

import org.wildfly.extension.grpc.GrpcSecurity;
import org.wildfly.security.auth.server.SecurityIdentity;

import io.grpc.stub.StreamObserver;
import messages.HelloReply;
import messages.HelloRequest;

import org.wildfly.feature.pack.grpc.test.helloworld.GreeterGrpc;

/**
 * Returns the Elytron {@link SecurityIdentity} principal name from the gRPC {@link io.grpc.Context},
 * or {@code "no-security-identity"} if none was propagated by the built-in interceptor.
 */
public class SecurityIdentityGreeterServiceImpl extends GreeterGrpc.GreeterImplBase {

    @Override
    public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
        final SecurityIdentity identity = GrpcSecurity.SECURITY_IDENTITY_KEY.get();
        final String principalName = identity != null
                ? identity.getPrincipal().getName()
                : "no-security-identity";
        responseObserver.onNext(HelloReply.newBuilder().setMessage(principalName).build());
        responseObserver.onCompleted();
    }
}

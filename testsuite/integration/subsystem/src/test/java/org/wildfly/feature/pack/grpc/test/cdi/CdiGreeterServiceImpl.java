/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.feature.pack.grpc.test.cdi;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.wildfly.feature.pack.grpc.test.helloworld.GreeterGrpc;

import io.grpc.stub.StreamObserver;
import messages.HelloReply;
import messages.HelloRequest;

@Dependent
public class CdiGreeterServiceImpl extends GreeterGrpc.GreeterImplBase {

    @Inject
    GreetingService greetingService;

    @Override
    public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
        String message = greetingService.greet(request.getName());
        responseObserver.onNext(HelloReply.newBuilder().setMessage(message).build());
        responseObserver.onCompleted();
    }
}

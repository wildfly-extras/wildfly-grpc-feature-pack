/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.grpc.example.helloworld;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import io.grpc.stub.StreamObserver;

@Dependent
public class GreeterServiceImpl extends GreeterGrpc.GreeterImplBase {

    @Inject
    GreetingService greetingService;

    @Override
    public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
        String message = greetingService.greet(request.getName());
        responseObserver.onNext(HelloReply.newBuilder().setMessage(message).build());
        responseObserver.onCompleted();
    }
}

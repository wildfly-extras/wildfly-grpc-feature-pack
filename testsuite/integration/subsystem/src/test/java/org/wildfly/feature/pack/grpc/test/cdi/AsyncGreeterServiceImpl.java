/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.feature.pack.grpc.test.cdi;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.wildfly.feature.pack.grpc.test.helloworld.GreeterGrpc;

import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;
import io.grpc.stub.StreamObserver;
import messages.HelloReply;
import messages.HelloRequest;

@ApplicationScoped
public class AsyncGreeterServiceImpl implements GreeterGrpc.AsyncService, BindableService {

    @Inject
    GreetingService greetingService;

    @Override
    public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
        String message = greetingService.greet(request.getName());
        responseObserver.onNext(HelloReply.newBuilder().setMessage(message).build());
        responseObserver.onCompleted();
    }

    @Override
    public ServerServiceDefinition bindService() {
        return GreeterGrpc.bindService(this);
    }
}

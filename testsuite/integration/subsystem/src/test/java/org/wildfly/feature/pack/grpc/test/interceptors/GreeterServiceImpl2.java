/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.feature.pack.grpc.test.interceptors;

import org.wildfly.extension.grpc.example.helloworld2.Greeter2Grpc.Greeter2ImplBase;
import org.wildfly.feature.pack.grpc.InterceptorTracker;

import io.grpc.stub.StreamObserver;
import messages.HelloReply;
import messages.HelloRequest;

/**
 * Retrieves the {@code String} flag from {@link InterceptorTracker}, appends it to
 * the {@code String} in the incoming {@link HelloRequest}, and returns the result in
 * a {@link HelloReply}. The result indicates the order
 * in which the {@link ServerInterceptor}s, if any, are executed.
 */
public class GreeterServiceImpl2 extends Greeter2ImplBase {

    @Override
    public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
        String name = request.getName();
        String message = "Hello " + name + InterceptorTracker.getFlag();
        InterceptorTracker.reset();
        responseObserver.onNext(HelloReply.newBuilder().setMessage(message).build());
        responseObserver.onCompleted();
    }
}

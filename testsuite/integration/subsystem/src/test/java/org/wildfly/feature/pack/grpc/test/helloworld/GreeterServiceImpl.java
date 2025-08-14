/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.feature.pack.grpc.test.helloworld;

import org.wildfly.feature.pack.grpc.InterceptorTracker;

import io.grpc.stub.StreamObserver;
import messages.HelloReply;
import messages.HelloRequest;

public class GreeterServiceImpl extends GreeterGrpc.GreeterImplBase {

    /**
     * Retrieves a value from {@link InterceptorTracker} to pass back to client
     * an indication of the order in which the {@link ServerInterceptor}s executed.
     */
    @Override
    public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
        String name = request.getName();
        String message = "Hello " + name + InterceptorTracker.getFlag();
        InterceptorTracker.reset();
        responseObserver.onNext(HelloReply.newBuilder().setMessage(message).build());
        responseObserver.onCompleted();
    }
}

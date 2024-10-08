/*
 *  Copyright 2022 Red Hat
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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

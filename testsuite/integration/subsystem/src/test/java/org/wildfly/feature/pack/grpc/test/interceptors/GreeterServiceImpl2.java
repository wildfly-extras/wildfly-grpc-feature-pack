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

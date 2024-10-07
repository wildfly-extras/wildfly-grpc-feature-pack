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

import java.util.List;

import org.wildfly.extension.grpc.example1.chat1.ChatService1Grpc.ChatService1ImplBase;
import org.wildfly.feature.pack.grpc.InterceptorTracker;

import chatmessages.ChatMessage;
import io.grpc.stub.StreamObserver;

/**
 * Retrieves the {@code Integer}s from {@link InterceptorTracker} and computes a
 * result based on each incoming {@link ChatMessage}. The result indicates the order
 * in which the {@link ServerInterceptor}s are executed.
 */
public class ChatServiceImpl1 extends ChatService1ImplBase {

    @Override
    public StreamObserver<ChatMessage> chat(final StreamObserver<ChatMessage> responseObserver) {
        return new StreamObserver<ChatMessage>() {
            @Override
            public void onNext(ChatMessage request) {
                List<Integer> list = InterceptorTracker.getList();
                Integer result = Integer.valueOf(request.getMessage());
                for (int i = 0; i < list.size(); i++) {
                    result = result * list.get(i) + list.get(i);
                }
                String s = Integer.toString(Integer.valueOf(result));
                ChatMessage cm = ChatMessage.newBuilder().setMessage(s).build();
                responseObserver.onNext(cm);
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }

            @Override
            public void onError(Throwable t) {
                throw new RuntimeException(t);
            }
        };
    }
}

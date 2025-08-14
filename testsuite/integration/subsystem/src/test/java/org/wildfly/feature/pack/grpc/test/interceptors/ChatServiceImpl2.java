/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.feature.pack.grpc.test.interceptors;

import java.util.List;

import org.wildfly.extension.grpc.example2.chat2.ChatService2Grpc.ChatService2ImplBase;
import org.wildfly.feature.pack.grpc.InterceptorTracker;

import chatmessages.ChatMessage;
import io.grpc.stub.StreamObserver;

/**
 * Retrieves the {@code Integer}s from {@link InterceptorTracker} and computes a
 * result based on each incoming {@link ChatMessage}. The result indicates the order
 * in which the {@link ServerInterceptor}s are executed.
 */
public class ChatServiceImpl2 extends ChatService2ImplBase {

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

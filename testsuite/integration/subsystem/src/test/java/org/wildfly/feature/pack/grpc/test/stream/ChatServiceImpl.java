/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.feature.pack.grpc.test.stream;

import org.wildfly.extension.grpc.example.chat.ChatServiceGrpc;

import chatmessages.ChatMessage;
import io.grpc.stub.StreamObserver;

public class ChatServiceImpl extends ChatServiceGrpc.ChatServiceImplBase {

    @Override
    public StreamObserver<ChatMessage> chat(final StreamObserver<ChatMessage> responseObserver) {
        return new StreamObserver<ChatMessage>() {
            @Override
            public void onNext(ChatMessage request) {
                String s = Integer.toString(Integer.valueOf(request.getMessage()) * 2);
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

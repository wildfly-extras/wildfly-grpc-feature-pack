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
package org.wildfly.extension.grpc.example.chat;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.wildfly.grpc.GrpcService;

import io.grpc.stub.StreamObserver;

import com.google.protobuf.Timestamp;

@GrpcService
public class ChatServiceImpl extends ChatServiceGrpc.ChatServiceImplBase {

    private static Set<StreamObserver<ChatMessageFromServer>> observers = ConcurrentHashMap.newKeySet();

    @Override
    public StreamObserver<ChatMessage> chat(StreamObserver<ChatMessageFromServer> responseObserver) {
        observers.add(responseObserver);

        return new StreamObserver<ChatMessage>() {
            @Override
            public void onNext(ChatMessage value) {
                System.out.println(value);
                ChatMessageFromServer message = ChatMessageFromServer.newBuilder().setMessage(value)
                        .setTimestamp(Timestamp.newBuilder().setSeconds(System.currentTimeMillis() / 1000)).build();

                for (StreamObserver<ChatMessageFromServer> observer : observers) {
                    observer.onNext(message);
                }
            }

            @Override
            public void onError(Throwable t) {
                // do something;
            }

            @Override
            public void onCompleted() {
                observers.remove(responseObserver);
            }
        };
    }
}

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
package org.wildfly.feature.pack.grpc.test.stream;

import org.wildfly.extension.grpc.example.chat.ChatMessage;
import org.wildfly.extension.grpc.example.chat.ChatServiceGrpc;

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

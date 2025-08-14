/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.feature.pack.grpc;

import jakarta.annotation.Priority;

import io.grpc.ForwardingServerCallListener.SimpleForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import messages.HelloRequest;

@Priority(-11)
public class TestServerInterceptor11 implements ServerInterceptor {

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            final Metadata requestHeaders,
            ServerCallHandler<ReqT, RespT> next) {

        ServerCall.Listener<ReqT> listener = next.startCall(call, requestHeaders);
        return new TestListener<ReqT>(listener);
    }

    static class TestListener<ReqT> extends SimpleForwardingServerCallListener<ReqT> {

        protected TestListener(ServerCall.Listener<ReqT> delegate) {
            super(delegate);
        }

        @Override
        public void onMessage(ReqT message) {
            HelloRequest request = (HelloRequest) message;
            messages.HelloRequest.Builder builder = messages.HelloRequest.newBuilder();
            @SuppressWarnings("unchecked")
            ReqT reqT = (ReqT) builder.setName("**" + request.getName()).build();
            delegate().onMessage(reqT);
        }
    }
}

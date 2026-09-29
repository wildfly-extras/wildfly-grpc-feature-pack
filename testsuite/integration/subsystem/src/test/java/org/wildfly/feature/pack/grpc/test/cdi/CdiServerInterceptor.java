/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.feature.pack.grpc.test.cdi;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import io.grpc.ForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import messages.HelloReply;

@Dependent
public class CdiServerInterceptor implements ServerInterceptor {

    @Inject
    PrefixService prefixService;

    @Override
    @SuppressWarnings("unchecked")
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata requestHeaders,
            ServerCallHandler<ReqT, RespT> next) {

        String prefix = prefixService.value();

        ServerCall<ReqT, RespT> wrappedCall = new ForwardingServerCall.SimpleForwardingServerCall<>(call) {
            @Override
            public void sendMessage(RespT message) {
                if (message instanceof HelloReply) {
                    HelloReply reply = (HelloReply) message;
                    message = (RespT) HelloReply.newBuilder().setMessage(prefix + reply.getMessage()).build();
                }
                super.sendMessage(message);
            }
        };
        return next.startCall(wrappedCall, requestHeaders);
    }
}

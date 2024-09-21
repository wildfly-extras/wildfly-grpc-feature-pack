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
package org.wildfly.feature.pack.grpc;

import org.wildfly.extension.grpc.Priority;

import io.grpc.ForwardingServerCall.SimpleForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

@Priority(0)
public class TestServerInterceptor0 implements ServerInterceptor {

    public TestServerInterceptor0() {
        System.out.println("creating " + this);
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            final Metadata requestHeaders,
            ServerCallHandler<ReqT, RespT> next) {

        InterceptorTracker.checkin("+000");

        return next.startCall(
                new SimpleForwardingServerCall<ReqT, RespT>(call) {

                    @Override
                    public void sendHeaders(Metadata responseHeaders) {
                        super.sendHeaders(responseHeaders);
                    }

                    @Override
                    public void sendMessage(RespT message) {
                        System.out.println("HEADERS:");
                        for (String s : requestHeaders.keys()) {
                            System.out.println("  " + s);
                        }
                        super.sendMessage(message);
                        // if (message instanceof HelloReply) {
                        // HelloReply reply = (HelloReply) message;
                        // String s = reply.getMessage() + InterceptorTracker.getFlag();
                        // System.out.println(this + " returning: " + s);
                        // reply = HelloReply.newBuilder().setMessage(s).build();
                        // super.sendMessage((RespT) reply);
                        // } else {
                        // super.sendMessage(message);
                        // }
                    }
                }, requestHeaders);

    }

}

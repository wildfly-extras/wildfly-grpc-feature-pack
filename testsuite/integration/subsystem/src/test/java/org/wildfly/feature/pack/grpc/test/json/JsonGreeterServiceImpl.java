/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.feature.pack.grpc.test.json;

import com.google.protobuf.util.JsonFormat;

import io.grpc.stub.StreamObserver;
import messages.HelloReply;
import messages.HelloRequest;

import org.wildfly.feature.pack.grpc.test.helloworld.GreeterGrpc;

/**
 * A gRPC service implementation that uses {@link JsonFormat} to serialize and deserialize
 * the request message. This verifies that JSON serialization (backed by Gson) is accessible
 * from a WildFly deployment.
 */
public class JsonGreeterServiceImpl extends GreeterGrpc.GreeterImplBase {

    @Override
    public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
        try {
            // Serialize request to JSON and parse it back to verify JsonFormat works
            String json = JsonFormat.printer().print(request);
            HelloRequest.Builder builder = HelloRequest.newBuilder();
            JsonFormat.parser().merge(json, builder);
            HelloRequest parsedRequest = builder.build();

            String name = parsedRequest.getName();
            responseObserver.onNext(HelloReply.newBuilder().setMessage("Hello " + name).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }
}

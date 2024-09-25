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
package org.wildfly.feature.pack.grpc.test.helloworld;

import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;

import io.grpc.ManagedChannel;
import messages.HelloReply;
import messages.HelloRequest;

/**
 * The subclasses of {@link HelloWorldParent} execute {@link #hello hello()}, which
 * does a simple call and response over connections with various security configurations.
 *
 */
public class HelloWorldParent {

    protected static final String TARGET = "localhost:9555";
    protected static final String TARGET_HOST = "localhost";
    protected static final int TARGET_PORT = 9555;

    protected static ManagedChannel channel = null;
    protected static GreeterGrpc.GreeterBlockingStub blockingStub;

    @AfterClass
    public static void afterClass() throws Exception {
        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    public void hello() {
        HelloRequest request = HelloRequest.newBuilder().setName("Bob").build();
        HelloReply reply = blockingStub.sayHello(request);
        Assert.assertEquals("Hello Bob", reply.getMessage());
    }
}

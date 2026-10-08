/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.feature.pack.grpc.test.json;

import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.feature.pack.grpc.test.helloworld.GreeterGrpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import messages.HelloReply;
import messages.HelloRequest;

/**
 * Verifies that JSON serialization (via {@code com.google.protobuf.util.JsonFormat}) works
 * inside a WildFly deployment, as enabled by the Gson dependency added to the
 * {@code com.google.protobuf} WildFly module.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class JsonSerializationTest {

    private static final String TARGET = "localhost:9555";

    private static ManagedChannel channel;
    private static GreeterGrpc.GreeterBlockingStub blockingStub;

    @Deployment
    public static Archive<?> createTestArchive() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "JsonSerializationTest.war");
        war.addClasses(JsonGreeterServiceImpl.class, GreeterGrpc.class);
        war.addPackage(HelloRequest.class.getPackage());
        return war;
    }

    @BeforeClass
    public static void beforeClass() {
        channel = ManagedChannelBuilder.forTarget(TARGET).usePlaintext().build();
        blockingStub = GreeterGrpc.newBlockingStub(channel);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    public void helloWithJsonSerialization() {
        HelloRequest request = HelloRequest.newBuilder().setName("Bob").build();
        HelloReply reply = blockingStub.sayHello(request);
        Assert.assertEquals("Hello Bob", reply.getMessage());
    }
}

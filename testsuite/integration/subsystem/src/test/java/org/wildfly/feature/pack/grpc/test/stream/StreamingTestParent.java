/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.feature.pack.grpc.test.stream;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.extension.grpc.example.chat.ChatServiceGrpc;

import chatmessages.ChatMessage;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;

/**
 * The subclasses of {@link StreamingTestParent} execute {@link #streamingTest streamingTest()}, which
 * does a bidirectional streaming interaction over connections with various security configurations.
 */
public abstract class StreamingTestParent {

    protected static final String TARGET = "localhost:9555";
    protected static final String TARGET_HOST = "localhost";
    protected static final int TARGET_PORT = 9555;

    protected static ManagedChannel channel = null;
    protected static ChatServiceGrpc.ChatServiceStub stub;
    protected int sum;

    @AfterClass
    public static void afterClass() throws Exception {
        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    public void streamingTest() throws InterruptedException {

        final CountDownLatch finishLatch = new CountDownLatch(1);
        StreamObserver<ChatMessage> responseObserver = new StreamObserver<ChatMessage>() {

            @Override
            public void onNext(ChatMessage message) {
                sum += Integer.valueOf(message.getMessage());
            }

            @Override
            public void onCompleted() {
                finishLatch.countDown();
            }

            @Override
            public void onError(Throwable t) {
                finishLatch.countDown();
            }
        };
        StreamObserver<ChatMessage> requestObserver = stub.chat(responseObserver);
        try {
            for (int i = 0; i < 5; i++) {
                requestObserver.onNext(ChatMessage.newBuilder().setMessage(Integer.toString(i)).build());
                if (finishLatch.getCount() == 0) {
                    return;
                }
            }
        } catch (RuntimeException e) {
            requestObserver.onError(e);
            throw e;
        }
        requestObserver.onCompleted();

        if (!finishLatch.await(1, TimeUnit.MINUTES)) {
            Assert.fail("test can not finish within 1 minute");
        }
        Assert.assertEquals(20, sum);
    }
}

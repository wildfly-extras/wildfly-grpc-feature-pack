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
package org.wildfly.feature.pack.grpc.test.interceptors;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.Testable;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.grpc.example.chat.ChatServiceGrpc;
import org.wildfly.extension.grpc.example.chat.ChatServiceGrpc.ChatServiceImplBase;
import org.wildfly.extension.grpc.example1.chat1.ChatService1Grpc;
import org.wildfly.extension.grpc.example1.chat1.ChatService1Grpc.ChatService1ImplBase;
import org.wildfly.extension.grpc.example2.chat2.ChatService2Grpc;
import org.wildfly.extension.grpc.example2.chat2.ChatService2Grpc.ChatService2ImplBase;
import org.wildfly.feature.pack.grpc.InterceptorTracker;
import org.wildfly.feature.pack.grpc.StreamingServerInterceptor;
import org.wildfly.feature.pack.grpc.StreamingServerInterceptor0;
import org.wildfly.feature.pack.grpc.StreamingServerInterceptor1;
import org.wildfly.feature.pack.grpc.test.stream.ChatServiceImpl;

import chatmessages.ChatMessage;
import chatmessages.ChatMessageOrBuilder;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

/**
 * Runs streaming calls to verify that {@link ServerInterceptor}s are
 * <ol>
 * <li>recognized in deployments;</li>
 * <li>segregated so that they apply only to methods in the same WAR; and</li>
 * <li>executed in order as determined by the {@link org.wildfly.extension.grpc.Priority} annotation.</li>
 * </ol>
 *
 * Three WARs are transferred to the server. war.war has no {@link ServerInterceptor}s, war1.war has one, and war2.war has
 * three. The service implementations keep track of which {@link ServerInterceptor}s are executed. Each of
 * {@link streamingTest streamingTest()}, {@link streamingTest1 streamingTest1()}, and {@link streamingTest2 streamingTest2()}
 * makes an invocation on {@link ChatServiceImpl ChatServiceImpl}, {@link ChatServiceImpl1 ChatServiceImpl1} and
 * {@link ChatServiceImpl2 ChatServiceImpl2} in war.war, war1.war, and war2.war respectively, and test
 * the returned {@code Integer}.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class StreamingInterceptorTest {

    protected static final String TARGET = "localhost:9555";

    protected static ManagedChannel channel;
    protected static ChatServiceGrpc.ChatServiceStub stub;
    protected static ChatService1Grpc.ChatService1Stub stub1;
    protected static ChatService2Grpc.ChatService2Stub stub2;

    protected int sum;

    @Deployment
    public static Archive<?> createTestArchive() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "war.war");
        war.addClass(ChatMessage.class);
        war.addClass(ChatMessageOrBuilder.class);
        war.addClass(ChatServiceImpl.class);
        war.addClass(ChatServiceGrpc.class);
        war.addClass(ChatServiceImplBase.class);
        war.addClass(org.wildfly.extension.grpc.example.chat.ChatServiceGrpc.AsyncService.class);
        war.addClass(InterceptorTracker.class);
        war.add(EmptyAsset.INSTANCE, Testable.MARKER_FILE_PATH);

        WebArchive war1 = ShrinkWrap.create(WebArchive.class, "war1.war");
        war1.addClass(ChatMessage.class);
        war1.addClass(ChatMessageOrBuilder.class);
        war1.addClass(ChatServiceImpl1.class);
        war1.addClass(ChatService1Grpc.class);
        war1.addClass(ChatService1ImplBase.class);
        war1.addClass(org.wildfly.extension.grpc.example1.chat1.ChatService1Grpc.AsyncService.class);
        war1.addClass(InterceptorTracker.class);
        war1.addClass(StreamingServerInterceptor0.class);

        WebArchive war2 = ShrinkWrap.create(WebArchive.class, "war2.war");
        war2.addClass(ChatMessage.class);
        war2.addClass(ChatMessageOrBuilder.class);
        war2.addClass(ChatServiceImpl2.class);
        war2.addClass(ChatService2Grpc.class);
        war2.addClass(ChatService2ImplBase.class);
        war2.addClass(org.wildfly.extension.grpc.example2.chat2.ChatService2Grpc.AsyncService.class);
        war2.addClass(StreamingServerInterceptor.class);
        war2.addClass(StreamingServerInterceptor0.class);
        war2.addClass(StreamingServerInterceptor1.class);
        war2.addClass(InterceptorTracker.class);

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "interceptor.ear");
        ear.addAsModule(war);
        ear.addAsModule(war1);
        ear.addAsModule(war2);

        ear.as(ZipExporter.class).exportTo(new File("/tmp/interceptor.ear"), true);
        return ear;
    }

    @BeforeClass
    public static void beforeClass() {
        channel = ManagedChannelBuilder.forTarget(TARGET).usePlaintext().build();
        stub = ChatServiceGrpc.newStub(channel);
        stub1 = ChatService1Grpc.newStub(channel);
        stub2 = ChatService2Grpc.newStub(channel);

    }

    @AfterClass
    public static void afterClass() throws Exception {
        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }

    /**
     * There are no interceptors in this WAR. The result is 0 * 2 + 1 * 2 + 2 * 2 + 3 * 2 + 4 * 2 = 20.
     */
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

    /**
     * <pre>
     * This WAR has {@link StreamingServerInterceptor0}, which adds 5 to the InterceptorTracker list.
     *
     * The result is
     *
     *    (0 * 5 + 5)
     *  + (1 * 5 + 5)
     *  + (2 * 5 + 5)
     *  + (3 * 5 + 5)
     *  + (4 * 5 + 5) = 75
     * </pre>
     */
    @Test
    public void streamingTest1() throws InterruptedException {

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
        StreamObserver<ChatMessage> requestObserver = stub1.chat(responseObserver);
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
        Assert.assertEquals(75, sum);
    }

    /**
     * <pre>
     * This WAR has {@link StreamingServerInterceptor0} (priority 0), which adds 5 to the {@link InterceptorTracker} list
     *              {@link StreamingServerInterceptor1} (priority 1), which adds 7 to the {@link InterceptorTracker} list
     *              {@link StreamingServerInterceptor}  (priority MAX_VALUE), which adds 3 to the {@link InterceptorTracker} list
     *
     * The result is
     *
     *    ((0 * 5 + 5) * 7 + 7) * 3 + 3 = 129
     *  + ((1 * 5 + 5) * 7 + 7) * 3 + 3 = 234
     *  + ((2 * 5 + 5) * 7 + 7) * 3 + 3 = 339
     *  + ((3 * 5 + 5) * 7 + 7) * 3 + 3 = 444
     *  + ((4 * 5 + 5) * 7 + 7) * 3 + 3 = 549 = 1695
     *
     *  <strong>Note.</strong> The {@link ServerInterceptor}s must be executed in the proper order.
     *  </pre>
     */
    @Test
    public void streamingTest2() throws InterruptedException {

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
        StreamObserver<ChatMessage> requestObserver = stub2.chat(responseObserver);
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
        Assert.assertEquals(1695, sum);
    }
}

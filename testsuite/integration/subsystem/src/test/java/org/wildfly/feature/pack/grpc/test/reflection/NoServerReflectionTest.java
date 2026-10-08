/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.feature.pack.grpc.test.reflection;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

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

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.reflection.v1.ServerReflectionGrpc;
import io.grpc.reflection.v1.ServerReflectionRequest;
import io.grpc.reflection.v1.ServerReflectionResponse;
import io.grpc.stub.StreamObserver;

/**
 * Verifies that gRPC server reflection is disabled by default.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class NoServerReflectionTest {

    private static final String TARGET = "localhost:9555";

    private static ManagedChannel channel;

    @Deployment
    public static Archive<?> createTestArchive() {
        return ShrinkWrap.create(WebArchive.class, "NoServerReflectionTest.war");
    }

    @BeforeClass
    public static void beforeClass() {
        channel = ManagedChannelBuilder.forTarget(TARGET).usePlaintext().build();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        channel.shutdownNow();
        if (!channel.awaitTermination(5, TimeUnit.SECONDS)) {
            System.err.println("WARNING: gRPC channel did not terminate within 5 seconds");
        }
    }

    @Test
    public void reflectionIsDisabledByDefault() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        ServerReflectionGrpc.ServerReflectionStub stub = ServerReflectionGrpc.newStub(channel);
        StreamObserver<ServerReflectionRequest> requestObserver = stub.serverReflectionInfo(
                new StreamObserver<>() {
                    @Override
                    public void onNext(ServerReflectionResponse response) {
                        latch.countDown();
                    }

                    @Override
                    public void onError(Throwable t) {
                        error.set(t);
                        latch.countDown();
                    }

                    @Override
                    public void onCompleted() {
                        latch.countDown();
                    }
                });
        requestObserver.onNext(ServerReflectionRequest.newBuilder().setListServices("").build());
        requestObserver.onCompleted();

        Assert.assertTrue("Timed out waiting for reflection response", latch.await(10, TimeUnit.SECONDS));
        Assert.assertNotNull("Expected an error when reflection is disabled", error.get());
        Assert.assertTrue("Expected StatusRuntimeException", error.get() instanceof StatusRuntimeException);
        StatusRuntimeException sre = (StatusRuntimeException) error.get();
        Status.Code code = sre.getStatus().getCode();
        // UNAVAILABLE can occur transiently if the server port is still rebinding after a prior reload
        Assert.assertTrue("Expected UNIMPLEMENTED (reflection disabled) or UNAVAILABLE (port rebinding), got: " + code,
                code == Status.Code.UNIMPLEMENTED || code == Status.Code.UNAVAILABLE);
    }
}

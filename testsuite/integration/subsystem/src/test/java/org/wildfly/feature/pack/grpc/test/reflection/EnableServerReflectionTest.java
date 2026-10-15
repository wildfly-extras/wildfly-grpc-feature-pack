/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.feature.pack.grpc.test.reflection;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.arquillian.setup.SnapshotServerSetupTask;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.feature.pack.grpc.InterceptorTracker;
import org.wildfly.feature.pack.grpc.test.helloworld.GreeterGrpc;
import org.wildfly.feature.pack.grpc.test.helloworld.GreeterServiceImpl;
import org.wildfly.feature.pack.grpc.test.helloworld.Helloworld;
import org.wildfly.feature.pack.grpc.test.utility.ServerReload;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.reflection.v1.ListServiceResponse;
import io.grpc.reflection.v1.ServerReflectionGrpc;
import io.grpc.reflection.v1.ServerReflectionRequest;
import io.grpc.reflection.v1.ServerReflectionResponse;
import io.grpc.reflection.v1.ServiceResponse;
import io.grpc.stub.StreamObserver;
import messages.HelloRequest;

/**
 * Verifies that gRPC server reflection lists deployed services when enabled via management attribute.
 */
@RunWith(Arquillian.class)
@ServerSetup(EnableServerReflectionTest.ServerReflectionSetupTask.class)
@RunAsClient
public class EnableServerReflectionTest {

    private static final String TARGET = "localhost:9555";
    private static final String EXPECTED_SERVICE = "helloworld.Greeter";

    private static ManagedChannel channel;

    public static class ServerReflectionSetupTask extends SnapshotServerSetupTask {

        @Override
        protected void doSetup(final ManagementClient client, final String containerId) throws Exception {
            ModelNode reloadOp = Operations.createOperation("reload-enhanced");
            reloadOp.get("stability").set("experimental");

            // Reload to experimental stability so the EXPERIMENTAL attribute becomes visible
            ServerReload.executeReloadAndWaitForCompletion(client.getControllerClient(), reloadOp);

            // Write the attribute (only visible at experimental stability)
            ModelNode address = Operations.createAddress("subsystem", "grpc");
            ModelNode op = Operations.createWriteAttributeOperation(address, "enable-server-reflection", true);
            ModelNode result = client.getControllerClient().execute(op);
            if (!Operations.isSuccessfulOutcome(result)) {
                throw new RuntimeException("Failed to enable server reflection: "
                        + Operations.getFailureDescription(result).asString());
            }

            // Reload again to apply the attribute change, keeping experimental stability
            ServerReload.executeReloadAndWaitForCompletion(client.getControllerClient(), reloadOp);
        }
    }

    @Deployment
    public static Archive<?> createTestArchive() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "EnableServerReflectionTest.war");
        war.addClasses(GreeterServiceImpl.class);
        war.addPackage(HelloRequest.class.getPackage());
        war.addClass(GreeterGrpc.class);
        war.addClass(Helloworld.class);
        war.addClass(InterceptorTracker.class);
        return war;
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
    public void reflectionListsDeployedServices() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        List<String> services = new ArrayList<>();
        AtomicReference<Throwable> error = new AtomicReference<>();

        ServerReflectionGrpc.ServerReflectionStub stub = ServerReflectionGrpc.newStub(channel);
        StreamObserver<ServerReflectionRequest> requestObserver = stub.serverReflectionInfo(
                new StreamObserver<>() {
                    @Override
                    public void onNext(ServerReflectionResponse response) {
                        if (response.hasListServicesResponse()) {
                            ListServiceResponse listResponse = response.getListServicesResponse();
                            for (ServiceResponse s : listResponse.getServiceList()) {
                                services.add(s.getName());
                            }
                        }
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
        Assert.assertNull("Unexpected error during reflection call: " + error.get(), error.get());
        Assert.assertTrue("Expected service '" + EXPECTED_SERVICE + "' in reflection response, got: " + services,
                services.contains(EXPECTED_SERVICE));
    }
}

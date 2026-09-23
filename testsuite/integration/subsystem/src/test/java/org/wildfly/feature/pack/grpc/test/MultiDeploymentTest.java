/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.feature.pack.grpc.test;

import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.Testable;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.grpc.example.helloworld1.Greeter1Grpc;
import org.wildfly.feature.pack.grpc.InterceptorTracker;
import org.wildfly.feature.pack.grpc.test.helloworld.GreeterGrpc;
import org.wildfly.feature.pack.grpc.test.helloworld.GreeterServiceImpl;
import org.wildfly.feature.pack.grpc.test.interceptors.GreeterServiceImpl1;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import messages.HelloReply;
import messages.HelloRequest;

/**
 * Tests that multiple deployments providing different gRPC services can coexist,
 * and that deploying a duplicate gRPC service name fails with an appropriate error.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class MultiDeploymentTest {

    private static final String TARGET = "localhost:9555";

    private static ManagedChannel channel;
    private static GreeterGrpc.GreeterBlockingStub greeterStub;
    private static Greeter1Grpc.Greeter1BlockingStub greeter1Stub;

    @ArquillianResource
    private Deployer deployer;

    /**
     * First deployment: the helloworld Greeter service.
     */
    @Deployment(name = "multi-war1")
    public static Archive<?> createWar1() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "multi-war1.war");
        war.addPackage(HelloRequest.class.getPackage());
        war.addClass(GreeterGrpc.class);
        war.addClass(GreeterServiceImpl.class);
        war.addClass(InterceptorTracker.class);
        war.add(EmptyAsset.INSTANCE, Testable.MARKER_FILE_PATH);
        return war;
    }

    /**
     * Second deployment: a different gRPC service (Greeter1). Should coexist with Greeter.
     */
    @Deployment(name = "multi-war2")
    public static Archive<?> createWar2() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "multi-war2.war");
        war.addPackage(HelloRequest.class.getPackage());
        war.addClass(Greeter1Grpc.class);
        war.addClass(GreeterServiceImpl1.class);
        war.addClass(InterceptorTracker.class);
        return war;
    }

    /**
     * Conflicting deployment: also provides the helloworld Greeter service.
     * Deployment must be manual (managed=false) so we can assert that it fails.
     */
    @Deployment(name = "multi-war-conflict", managed = false)
    public static Archive<?> createConflictingWar() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "multi-war-conflict.war");
        war.addPackage(HelloRequest.class.getPackage());
        war.addClass(GreeterGrpc.class);
        war.addClass(GreeterServiceImpl.class);
        war.addClass(InterceptorTracker.class);
        return war;
    }

    @BeforeClass
    public static void beforeClass() {
        channel = ManagedChannelBuilder.forTarget(TARGET).usePlaintext().build();
        greeterStub = GreeterGrpc.newBlockingStub(channel);
        greeter1Stub = Greeter1Grpc.newBlockingStub(channel);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }

    /**
     * Verifies that two deployments providing different gRPC services can coexist and
     * each service is callable independently.
     */
    @Test
    public void testDifferentServicesCoexist() {
        HelloRequest request = HelloRequest.newBuilder().setName("Alice").build();

        HelloReply reply1 = greeterStub.sayHello(request);
        Assert.assertEquals("Hello Alice", reply1.getMessage());

        HelloReply reply2 = greeter1Stub.sayHello(request);
        Assert.assertEquals("Hello Alice", reply2.getMessage());
    }

    /**
     * Verifies that attempting to deploy a second WAR that registers a gRPC service
     * already registered by another deployment fails with an appropriate error.
     */
    @Test
    public void testDuplicateServiceFails() {
        try {
            deployer.deploy("multi-war-conflict");
            // If we reach here the deployment did not fail — clean up and fail the test
            deployer.undeploy("multi-war-conflict");
            Assert.fail("Expected deployment to fail because the Greeter gRPC service is already registered by multi-war1.war");
        } catch (Exception expected) {
            // The deployment was correctly rejected due to the duplicate service name
        }
    }
}

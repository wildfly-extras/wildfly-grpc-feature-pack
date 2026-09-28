/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.feature.pack.grpc.test.cdi;

import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
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
 * Verifies that {@link io.grpc.ServerInterceptor}s are instantiated as CDI beans
 * when the deployment is a CDI-enabled archive (contains beans.xml).
 *
 * The {@link CdiServerInterceptor} injects a {@link PrefixService} CDI bean and
 * uses it to prepend the prefix to the response message, proving the interceptor was CDI-managed.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class CdiInterceptorTest {

    private static final String TARGET = "localhost:9555";

    private static ManagedChannel channel;
    private static GreeterGrpc.GreeterBlockingStub blockingStub;

    @Deployment
    public static Archive<?> createTestArchive() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "CdiInterceptorTest.war");
        war.addClasses(
                CdiInterceptorTest.class,
                CdiInterceptorGreeterServiceImpl.class,
                CdiServerInterceptor.class,
                PrefixService.class,
                GreetingService.class);
        war.addPackage(HelloRequest.class.getPackage());
        war.addClass(GreeterGrpc.class);
        war.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return war;
    }

    @BeforeClass
    public static void beforeClass() {
        channel = ManagedChannelBuilder.forTarget(TARGET).usePlaintext().build();
        blockingStub = GreeterGrpc.newBlockingStub(channel);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (channel != null) {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    public void interceptorIsCdiManaged() {
        HelloRequest request = HelloRequest.newBuilder().setName("WildFly").build();
        HelloReply reply = blockingStub.sayHello(request);
        // "[CDI]" prefix from PrefixService via CdiServerInterceptor + "CDI Hello WildFly" from GreetingService
        Assert.assertEquals("[CDI]CDI Hello WildFly", reply.getMessage());
    }
}

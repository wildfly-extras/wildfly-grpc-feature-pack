/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.feature.pack.grpc.test.coexistence;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
import org.wildfly.feature.pack.grpc.InterceptorTracker;
import org.wildfly.feature.pack.grpc.test.helloworld.GreeterGrpc;
import org.wildfly.feature.pack.grpc.test.helloworld.GreeterServiceImpl;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import messages.HelloReply;
import messages.HelloRequest;

/**
 * Verifies that gRPC routing does not interfere with regular HTTP endpoints.
 * <p>
 * The deployment exposes both a gRPC service ({@code helloworld.Greeter}) and a JAX-RS endpoint
 * at a path whose name mirrors the gRPC service ({@code /helloworld.Greeter/SayHello}).
 * <ul>
 * <li>A request with {@code Content-Type: application/grpc} must be routed to the gRPC service.</li>
 * <li>A plain HTTP GET to the WAR's context path must be routed to the JAX-RS endpoint,
 * not intercepted by the gRPC handler.</li>
 * </ul>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class GrpcHttpCoexistenceTest {

    private static final String GRPC_TARGET = "localhost:8080";
    private static final String HTTP_BASE = "http://localhost:8080";
    // WAR name without extension becomes the context path
    private static final String CONTEXT_PATH = "/GrpcHttpCoexistenceTest";

    private static ManagedChannel channel;
    private static GreeterGrpc.GreeterBlockingStub grpcStub;

    @Deployment
    public static Archive<?> createTestArchive() {
        return ShrinkWrap.create(WebArchive.class, "GrpcHttpCoexistenceTest.war")
                .addClasses(GreeterServiceImpl.class, GreeterGrpc.class)
                .addClasses(GreeterHttpEndpoint.class, JaxRsApplication.class)
                .addClass(InterceptorTracker.class)
                .addPackage(HelloRequest.class.getPackage())
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @BeforeClass
    public static void beforeClass() {
        channel = ManagedChannelBuilder.forTarget(GRPC_TARGET).usePlaintext().build();
        grpcStub = GreeterGrpc.newBlockingStub(channel);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    public void grpcRequestIsRoutedToGrpcService() {
        HelloReply reply = grpcStub.sayHello(HelloRequest.newBuilder().setName("gRPC").build());
        Assert.assertEquals("Hello gRPC", reply.getMessage());
    }

    @Test
    public void httpRequestIsRoutedToJaxRsEndpoint() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(HTTP_BASE + CONTEXT_PATH + "/helloworld.Greeter/SayHello?name=HTTP"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        Assert.assertEquals(200, response.statusCode());
        Assert.assertEquals("HTTP Hello HTTP", response.body());
    }
}

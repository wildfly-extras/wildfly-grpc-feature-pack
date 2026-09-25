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
 * Verifies gRPC / HTTP coexistence when the web application is deployed as ROOT.war,
 * i.e. at the root context path {@code /}.
 * <p>
 * ROOT.war and the gRPC subsystem share the same context path. The gRPC subsystem
 * registers its routing handler via {@code UndertowFilter}, which wraps the host's
 * root handler above Undertow's path router. This means:
 * <ul>
 * <li>Requests with {@code Content-Type: application/grpc} are intercepted before
 * path dispatch and routed to the gRPC servlet.</li>
 * <li>All other requests pass through to the underlying handler chain and are served
 * by ROOT.war's JAX-RS endpoint at the same path.</li>
 * </ul>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class GrpcRootWarCoexistenceTest {

    private static final String GRPC_TARGET = "localhost:8080";
    private static final String HTTP_BASE = "http://localhost:8080";

    private static ManagedChannel channel;
    private static GreeterGrpc.GreeterBlockingStub grpcStub;

    @Deployment
    public static Archive<?> createTestArchive() {
        // ROOT.war deploys at context path "/" — same root as the gRPC subsystem handler
        return ShrinkWrap.create(WebArchive.class, "ROOT.war")
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
    public void httpRequestIsRoutedToRootWar() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        // ROOT.war is at "/", so JAX-RS endpoint is directly at "/helloworld.Greeter/SayHello"
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(HTTP_BASE + "/helloworld.Greeter/SayHello?name=HTTP"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        Assert.assertEquals(200, response.statusCode());
        Assert.assertEquals("HTTP Hello HTTP", response.body());
    }
}

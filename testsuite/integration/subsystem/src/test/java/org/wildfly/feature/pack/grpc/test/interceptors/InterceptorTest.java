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
import org.wildfly.extension.grpc.example.helloworld1.Greeter1Grpc;
import org.wildfly.extension.grpc.example.helloworld2.Greeter2Grpc;
import org.wildfly.feature.pack.grpc.InterceptorTracker;
import org.wildfly.feature.pack.grpc.TestServerInterceptor;
import org.wildfly.feature.pack.grpc.TestServerInterceptor0;
import org.wildfly.feature.pack.grpc.TestServerInterceptor1;
import org.wildfly.feature.pack.grpc.TestServerInterceptor10;
import org.wildfly.feature.pack.grpc.TestServerInterceptor11;
import org.wildfly.feature.pack.grpc.test.helloworld.GreeterGrpc;
import org.wildfly.feature.pack.grpc.test.helloworld.GreeterServiceImpl;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import messages.HelloReply;
import messages.HelloRequest;

/**
 * Run simple unary calls to verify that {@link ServerInterceptor}s are
 * <ol>
 * <li>recognized in deployments;</li>
 * <li>segregated so that they apply only to methods in the same WAR; and</li>
 * <li>executed in order as determined by the {@link org.wildfly.extension.grpc.Priority} annotation.</li>
 * </ol>
 *
 * Three WARs are transferred to the server. war.war has no {@link ServerInterceptor}s, war1.war has one, and war2.war has
 * five. The service implementations keep track of which {@link ServerInterceptor}s are executed. Each of
 * {@link hello hello()}, {@link hello1 hello1()}, and {@link hello2 hello2()} makes an invocation on
 * {@link GreeterServiceImpl GreeterServiceImpl}, {@link GreeterServiceImpl1 GreeterServiceImpl1} and
 * {@link GreeterServiceImpl2 GreeterServiceImpl2} in war.war, war1.war, and war2.war respectively, and test
 * the returned {@code String}.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class InterceptorTest {
    protected static final String TARGET = "localhost:9555";

    protected static GreeterGrpc.GreeterBlockingStub blockingStub;
    protected static Greeter1Grpc.Greeter1BlockingStub blockingStub1;
    protected static Greeter2Grpc.Greeter2BlockingStub blockingStub2;
    protected static ManagedChannel channel = null;

    @Deployment
    public static Archive<?> createTestArchive() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "war.war");
        war.addPackage(HelloRequest.class.getPackage());
        war.addClass(HelloRequest.class);
        war.addClass(HelloReply.class);
        war.addClass(GreeterGrpc.class);
        war.addClass(GreeterServiceImpl.class);
        war.addClass(InterceptorTracker.class);
        war.add(EmptyAsset.INSTANCE, Testable.MARKER_FILE_PATH);

        WebArchive war1 = ShrinkWrap.create(WebArchive.class, "war1.war");
        war1.addPackage(HelloRequest.class.getPackage());
        war1.addClass(HelloRequest.class);
        war1.addClass(HelloReply.class);
        war1.addClass(Greeter1Grpc.class);
        war1.addClass(GreeterServiceImpl1.class);
        war1.addClass(InterceptorTracker.class);
        war1.addClass(TestServerInterceptor1.class);

        WebArchive war2 = ShrinkWrap.create(WebArchive.class, "war2.war");
        war2.addPackage(HelloRequest.class.getPackage());
        war2.addClass(HelloRequest.class);
        war2.addClass(HelloReply.class);
        war2.addClass(Greeter2Grpc.class);
        war2.addClass(GreeterServiceImpl2.class);
        war2.addClass(TestServerInterceptor.class);
        war2.addClass(TestServerInterceptor0.class);
        war2.addClass(TestServerInterceptor1.class);
        war2.addClass(TestServerInterceptor10.class);
        war2.addClass(TestServerInterceptor11.class);
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
        blockingStub = GreeterGrpc.newBlockingStub(channel);
        blockingStub1 = Greeter1Grpc.newBlockingStub(channel);
        blockingStub2 = Greeter2Grpc.newBlockingStub(channel);

    }

    @AfterClass
    public static void afterClass() throws Exception {
        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }

    /**
     * There are no {@link ServerInterceptor}s in war.war.
     */
    @Test
    public void hello() {
        HelloRequest request = HelloRequest.newBuilder().setName("Bob").build();
        HelloReply reply = blockingStub.sayHello(request);
        Assert.assertEquals("Hello Bob", reply.getMessage());
    }

    /**
     * There is one {@link ServerInterceptor}s in war1.war.
     */
    @Test
    public void hello1() {
        HelloRequest request = HelloRequest.newBuilder().setName("Bob").build();
        HelloReply reply = blockingStub1.sayHello(request);
        Assert.assertEquals("Hello Bob+111", reply.getMessage());
    }

    /**
     * There are three {@link ServerInterceptor}s in war2.war.
     */
    @Test
    public void hello2() {
        HelloRequest request = HelloRequest.newBuilder().setName("Bob").build();
        HelloReply reply = blockingStub2.sayHello(request);
        Assert.assertEquals("Hello !!**Bob+000+111+xxx", reply.getMessage());
    }
}

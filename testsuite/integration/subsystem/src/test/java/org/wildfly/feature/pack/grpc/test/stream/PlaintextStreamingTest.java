/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.feature.pack.grpc.test.stream;

import java.io.File;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.wildfly.extension.grpc.example.chat.ChatServiceGrpc;

import chatmessages.ChatMessage;
import io.grpc.ManagedChannelBuilder;

/**
 * Executes {@link StreamingTestParent#streamingTest() StreamingTestParent.streamingTest()} over a plaintext connection.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class PlaintextStreamingTest extends StreamingTestParent {

    @Deployment
    public static Archive<?> createTestArchive() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "PlaintextStreamingTest.war");
        war.addClasses(PlaintextStreamingTest.class, ChatServiceImpl.class, ChatServiceGrpc.class);
        war.addPackage(ChatMessage.class.getPackage());
        war.as(ZipExporter.class).exportTo(new File("/tmp/hello.war"), true);
        return war;
    }

    @BeforeClass
    public static void beforeClass() {
        channel = ManagedChannelBuilder.forTarget(TARGET).usePlaintext().build();
        stub = ChatServiceGrpc.newStub(channel);
    }
}

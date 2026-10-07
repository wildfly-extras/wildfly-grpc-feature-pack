/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.feature.pack.grpc.test.stream;

import java.io.InputStream;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.wildfly.extension.grpc.example.chat.ChatServiceGrpc;

import chatmessages.ChatMessage;
import io.grpc.ChannelCredentials;
import io.grpc.Grpc;
import io.grpc.TlsChannelCredentials;

/**
 * Executes {@link StreamingTestParent#streamingTest() StreamingTestParent.streamingTest()}
 * over a connection configured with a keystore on the server side and a truststore on the client side.
 * The server is provisioned with one-way TLS enabled via configure-elytron-tls.cli.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class OnewaySecureStreamingTest extends StreamingTestParent {

    @Deployment
    public static Archive<?> createTestArchive() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "OnewaySecureStreamingTest.war");
        war.addClasses(OnewaySecureStreamingTest.class, ChatServiceImpl.class, ChatServiceGrpc.class);
        war.addPackage(ChatMessage.class.getPackage());
        return war;
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        InputStream trustStore = OnewaySecureStreamingTest.class.getClassLoader().getResourceAsStream("client.truststore.pem");
        ChannelCredentials creds = TlsChannelCredentials.newBuilder().trustManager(trustStore).build();
        channel = Grpc.newChannelBuilderForAddress(TARGET_HOST, SECURE_PORT, creds).build();
        stub = ChatServiceGrpc.newStub(channel);
    }
}

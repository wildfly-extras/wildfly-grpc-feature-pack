/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.feature.pack.grpc.test.stream;

import java.io.InputStream;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.arquillian.setup.SnapshotServerSetupTask;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.client.helpers.Operations.CompositeOperationBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.wildfly.feature.pack.grpc.test.utility.ServerReload;
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
 * over a connection configured with a keystore on both the server side and the client side and a truststore
 * on both the server and client side.
 * The server is provisioned with one-way TLS; this test adds mutual TLS at runtime.
 */
@RunWith(Arquillian.class)
@ServerSetup(TwowaySecureStreamingTest.MutualTlsServerSetupTask.class)
@RunAsClient
public class TwowaySecureStreamingTest extends StreamingTestParent {

    public static class MutualTlsServerSetupTask extends SnapshotServerSetupTask {

        @Override
        protected void doSetup(final ManagementClient client, final String containerId) throws Exception {
            final CompositeOperationBuilder builder = CompositeOperationBuilder.create();
            final ModelNode credentialRef = new ModelNode();
            credentialRef.get("clear-text").set("secret");

            ModelNode address = Operations.createAddress("subsystem", "elytron", "key-store",
                    "trust-store-eeeecd12-36f9-4156-92c7-a889383f17a1");
            ModelNode op = Operations.createAddOperation(address);
            op.get("credential-reference").set(credentialRef);
            op.get("type").set("PKCS12");
            op.get("path").set("server.truststore.p12");
            op.get("relative-to").set("jboss.server.config.dir");
            op.get("required").set(false);
            builder.addStep(op);

            address = Operations.createAddress("subsystem", "elytron", "trust-manager",
                    "trust-manager-eeeecd12-36f9-4156-92c7-a889383f17a1");
            op = Operations.createAddOperation(address);
            op.get("key-store").set("trust-store-eeeecd12-36f9-4156-92c7-a889383f17a1");
            builder.addStep(op);

            address = Operations.createAddress("subsystem", "elytron", "server-ssl-context",
                    "ssl-context-afcdd1f8-d1a7-4137-aa13-c45237e32428");
            builder.addStep(Operations.createWriteAttributeOperation(address, "need-client-auth", new ModelNode(true)));
            builder.addStep(Operations.createWriteAttributeOperation(address, "trust-manager",
                    new ModelNode("trust-manager-eeeecd12-36f9-4156-92c7-a889383f17a1")));

            final var result = client.getControllerClient().execute(builder.build());
            if (!Operations.isSuccessfulOutcome(result)) {
                throw new RuntimeException("Failed to enable mutual TLS: " + Operations.getFailureDescription(result));
            }
            ServerReload.reloadIfRequired(client.getControllerClient());
        }
    }

    @Deployment
    public static Archive<?> createTestArchive() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "TwowaySecureStreamingTest.war");
        war.addClasses(TwowaySecureStreamingTest.class, ChatServiceImpl.class, ChatServiceGrpc.class);
        war.addPackage(ChatMessage.class.getPackage());
        return war;
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        ClassLoader classLoader = TwowaySecureStreamingTest.class.getClassLoader();
        KeyStore clientKeyStore = KeyStore.getInstance("PKCS12");
        try (InputStream clientKeyStoreStream = classLoader.getResourceAsStream("client.keystore.p12")) {
            clientKeyStore.load(clientKeyStoreStream, "secret".toCharArray());
        }
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(clientKeyStore, "secret".toCharArray());
        try (InputStream trustStore = classLoader.getResourceAsStream("client.truststore.pem")) {
            ChannelCredentials creds = TlsChannelCredentials.newBuilder()
                    .trustManager(trustStore)
                    .keyManager(kmf.getKeyManagers())
                    .build();
            channel = Grpc.newChannelBuilderForAddress(TARGET_HOST, SECURE_PORT, creds).build();
        }
        stub = ChatServiceGrpc.newStub(channel);
    }
}

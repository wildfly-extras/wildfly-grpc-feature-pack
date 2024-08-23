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
package org.wildfly.feature.pack.grpc.test.helloworld;

import java.io.File;
import java.io.InputStream;

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
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.wildfly.feature.pack.grpc.test.utility.ServerReload;

import io.grpc.ChannelCredentials;
import io.grpc.Grpc;
import io.grpc.TlsChannelCredentials;

@RunWith(Arquillian.class)
@ServerSetup(OnewaySecureHelloWorldTest.SslServerSetupTask.class)
@RunAsClient
public class OnewaySecureHelloWorldTest extends HelloWorldParent {

    public static class SslServerSetupTask extends SnapshotServerSetupTask {

        @Override
        protected void doSetup(final ManagementClient client, final String containerId) throws Exception {
            secureServer(client);
        }
    }

    protected static void secureServer(final ManagementClient client) throws Exception {
        final CompositeOperationBuilder builder = CompositeOperationBuilder.create();

        // /subsystem=elytron/key-store=grpc-key-store:add(credential-reference={clear-text="secret"}, type=JKS,
        // path="server.keystore.jks", relative-to="jboss.server.config.dir", required=false)
        ModelNode address = Operations.createAddress("subsystem", "elytron", "key-store", "grpc-key-store");
        ModelNode op = Operations.createAddOperation(address);
        final ModelNode credentialRef = new ModelNode();
        credentialRef.get("clear-text").set("secret");
        op.get("credential-reference").set(credentialRef);
        op.get("type").set("JKS");
        op.get("path").set("../../../ssl/server.keystore.jks");
        // op.get("relative-to").set("jboss.server.config.dir");
        op.get("required").set(false);
        builder.addStep(op);

        // /subsystem=elytron/key-manager=grpc-key-manager:add(key-store=grpc-key-store,
        // credential-reference={clear-text="secret"})
        address = Operations.createAddress("subsystem", "elytron", "key-manager", "grpc-key-manager");
        op = Operations.createAddOperation(address);
        op.get("key-store").set("grpc-key-store");
        op.get("credential-reference").set(credentialRef);
        builder.addStep(op);

        // /subsystem=elytron/server-ssl-context=grpc-ssl-context:add(cipher-suite-filter=DEFAULT, protocols=["TLSv1.2"],
        // want-client-auth="false", need-client-auth="true", authentication-optional="false",
        // use-cipher-suites-order="false", key-manager="grpc-key-manager",
        // trust-manager="grpc-key-store-trust-manager")
        address = Operations.createAddress("subsystem", "elytron", "server-ssl-context", "grpc-ssl-context");
        op = Operations.createAddOperation(address);
        op.get("cipher-suite-filter").set("DEFAULT");
        final ModelNode protocols = new ModelNode().setEmptyList();
        protocols.add("TLSv1.2");
        op.get("protocols").set(protocols);
        op.get("want-client-auth").set(false);
        op.get("need-client-auth").set(true);
        op.get("authentication-optional").set(false);
        op.get("use-cipher-suites-order").set(false);
        op.get("key-manager").set("grpc-key-manager");
        builder.addStep(op);

        // /subsystem=undertow/server=default-server/https-listener=https:add(socket-binding=https,
        // ssl-context="grpc-ssl-context")
        address = Operations.createAddress("subsystem", "undertow", "server", "default-server", "https-listener", "https");
        op = Operations.createAddOperation(address);
        op.get("socket-binding").set("https");
        op.get("ssl-context").set("grpc-ssl-context");
        builder.addStep(op);

        // /subsystem=grpc:write-attribute(name=key-manager-name, value="grpc-key-manager")
        address = Operations.createAddress("subsystem", "grpc");
        builder.addStep(Operations.createWriteAttributeOperation(address, "key-manager-name", "grpc-key-manager"));

        final var result = client.getControllerClient().execute(builder.build());
        if (!Operations.isSuccessfulOutcome(result)) {
            throw new RuntimeException("Failed to configure SSL context: " + Operations.getFailureDescription(result));
        }
        ServerReload.reloadIfRequired(client.getControllerClient());
    }

    @Deployment
    public static Archive<?> createTestArchive() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "TestClient.war");
        war.addClasses(OnewaySecureHelloWorldTest.class, GreeterServiceImpl.class);
        war.addPackage(HelloRequest.class.getPackage());
        war.addClass(GreeterGrpc.class);
        war.addAsWebInfResource("web.xml");
//        war.as(ZipExporter.class).exportTo(
//                new File("/tmp/hello.war"), true);
        return war;
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        InputStream trustStore = OnewaySecureHelloWorldTest.class.getClassLoader().getResourceAsStream("client.truststore.pem");
        ChannelCredentials creds = TlsChannelCredentials.newBuilder().trustManager(trustStore).build();
        channel = Grpc.newChannelBuilderForAddress(TARGET_HOST, TARGET_PORT, creds).build();
        blockingStub = GreeterGrpc.newBlockingStub(channel);
    }
}

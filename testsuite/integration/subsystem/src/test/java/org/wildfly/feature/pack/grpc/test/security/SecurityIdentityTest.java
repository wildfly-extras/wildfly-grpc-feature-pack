/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.feature.pack.grpc.test.security;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

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
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.feature.pack.grpc.test.helloworld.GreeterGrpc;
import org.wildfly.feature.pack.grpc.test.utility.ServerReload;

import io.grpc.CallCredentials;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import messages.HelloReply;
import messages.HelloRequest;

/**
 * Verifies that the built-in {@code ElytronSecurityIdentityInterceptor} propagates an
 * authenticated Elytron {@link org.wildfly.security.auth.server.SecurityIdentity} into
 * the gRPC {@link io.grpc.Context} when the WAR has a security domain configured.
 *
 * <p>The test configures a real Elytron filesystem-realm with user {@code alice}, wires a
 * BASIC-auth HTTP authentication factory into Undertow's {@code application-security-domain},
 * then makes gRPC calls with and without {@code Authorization: Basic} credentials and asserts
 * the result.
 */
@RunWith(Arquillian.class)
@ServerSetup(SecurityIdentityTest.ElytronSecuritySetupTask.class)
@RunAsClient
public class SecurityIdentityTest {

    static final String REALM_NAME = "grpc-security-test-realm";
    static final String DOMAIN_NAME = "grpc-security-test-domain";
    static final String HTTP_AUTH_FACTORY_NAME = "grpc-security-test-http-auth";
    static final String TEST_USER = "alice";
    static final String TEST_PASSWORD = "password";

    private static ManagedChannel channel;
    private static GreeterGrpc.GreeterBlockingStub blockingStub;

    // -------------------------------------------------------------------------
    // Server setup
    // -------------------------------------------------------------------------

    public static class ElytronSecuritySetupTask extends SnapshotServerSetupTask {

        @Override
        protected void doSetup(ManagementClient client, String containerId) throws Exception {
            final ModelNode realmAddress = Operations.createAddress(
                    "subsystem", "elytron", "filesystem-realm", REALM_NAME);

            // Step 1: Elytron realm + security domain + HTTP auth factory + wire into /subsystem=grpc
            final CompositeOperationBuilder builder = CompositeOperationBuilder.create();

            ModelNode op = Operations.createAddOperation(realmAddress);
            op.get("path").set(REALM_NAME);
            op.get("relative-to").set("jboss.server.data.dir");
            builder.addStep(op);

            ModelNode domainAddress = Operations.createAddress(
                    "subsystem", "elytron", "security-domain", DOMAIN_NAME);
            op = Operations.createAddOperation(domainAddress);
            op.get("default-realm").set(REALM_NAME);
            op.get("permission-mapper").set("default-permission-mapper");
            op.get("realms").add().get("realm").set(REALM_NAME);
            builder.addStep(op);

            ModelNode factoryAddress = Operations.createAddress(
                    "subsystem", "elytron", "http-authentication-factory", HTTP_AUTH_FACTORY_NAME);
            op = Operations.createAddOperation(factoryAddress);
            op.get("http-server-mechanism-factory").set("global");
            op.get("security-domain").set(DOMAIN_NAME);
            ModelNode mechConfig = op.get("mechanism-configurations").add();
            mechConfig.get("mechanism-name").set("BASIC");
            mechConfig.get("mechanism-realm-configurations").add().get("realm-name").set(REALM_NAME);
            builder.addStep(op);

            // Wire the security domain into Undertow's application-security-domain mapping.
            // override-deployment-config=true forces PRO_ACTIVE authentication so credentials
            // in HTTP headers (including gRPC metadata) are processed on every request.
            ModelNode appDomainAddress = Operations.createAddress(
                    "subsystem", "undertow", "application-security-domain", DOMAIN_NAME);
            op = Operations.createAddOperation(appDomainAddress);
            op.get("http-authentication-factory").set(HTTP_AUTH_FACTORY_NAME);
            op.get("override-deployment-config").set(true);
            builder.addStep(op);

            ModelNode compositeResult = client.getControllerClient().execute(builder.build());
            if (!Operations.isSuccessfulOutcome(compositeResult)) {
                throw new RuntimeException("Failed to set up gRPC security: "
                        + Operations.getFailureDescription(compositeResult));
            }

            // Step 2: Add test identity outside the composite — SnapshotServerSetupTask restores
            // the Elytron config but not the filesystem-realm data directory, so a leftover
            // identity from a previous run would cause "identity already exists".
            ModelNode removeOp = Operations.createOperation("remove-identity", realmAddress);
            removeOp.get("identity").set(TEST_USER);
            client.getControllerClient().execute(removeOp); // ignore outcome

            ModelNode addOp = Operations.createOperation("add-identity", realmAddress);
            addOp.get("identity").set(TEST_USER);
            ModelNode addResult = client.getControllerClient().execute(addOp);
            if (!Operations.isSuccessfulOutcome(addResult)) {
                throw new RuntimeException("Failed to add identity: "
                        + Operations.getFailureDescription(addResult));
            }

            ModelNode pwOp = Operations.createOperation("set-password", realmAddress);
            pwOp.get("identity").set(TEST_USER);
            pwOp.get("clear").get("password").set(TEST_PASSWORD);
            ModelNode pwResult = client.getControllerClient().execute(pwOp);
            if (!Operations.isSuccessfulOutcome(pwResult)) {
                throw new RuntimeException("Failed to set password: "
                        + Operations.getFailureDescription(pwResult));
            }

            ServerReload.executeReloadAndWaitForCompletion(client.getControllerClient());
        }
    }

    // -------------------------------------------------------------------------
    // Deployment
    // -------------------------------------------------------------------------

    private static final String JBOSS_WEB_XML =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<jboss-web>\n"
            + "    <security-domain>" + DOMAIN_NAME + "</security-domain>\n"
            + "</jboss-web>\n";

    private static final String WEB_XML =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<web-app xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" version=\"6.0\">\n"
            + "    <login-config>\n"
            + "        <auth-method>BASIC</auth-method>\n"
            + "        <realm-name>" + REALM_NAME + "</realm-name>\n"
            + "    </login-config>\n"
            + "</web-app>\n";

    @Deployment
    public static Archive<?> createTestArchive() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "SecurityIdentityTest.war");
        war.addClasses(SecurityIdentityGreeterServiceImpl.class);
        war.addPackage(HelloRequest.class.getPackage());
        war.addClass(GreeterGrpc.class);
        war.addAsWebInfResource(new StringAsset(WEB_XML), "web.xml");
        war.addAsWebInfResource(new StringAsset(JBOSS_WEB_XML), "jboss-web.xml");
        return war;
    }

    // -------------------------------------------------------------------------
    // Channel lifecycle
    // -------------------------------------------------------------------------

    @BeforeClass
    public static void beforeClass() {
        channel = ManagedChannelBuilder.forTarget("127.0.0.1:8080").usePlaintext().build();
        blockingStub = GreeterGrpc.newBlockingStub(channel);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (channel != null) {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    public void securityIdentityIsAvailableForAuthenticatedUser() {
        HelloReply reply = blockingStub
                .withCallCredentials(basicAuth(TEST_USER, TEST_PASSWORD))
                .sayHello(HelloRequest.newBuilder().setName("test").build());
        Assert.assertEquals(
                "Authenticated gRPC call must expose the caller's principal name via SECURITY_IDENTITY_KEY",
                TEST_USER, reply.getMessage());
    }

    @Test
    public void securityIdentityIsAnonymousForUnauthenticatedRequest() {
        HelloReply reply = blockingStub.sayHello(HelloRequest.newBuilder().setName("test").build());
        Assert.assertEquals(
                "Unauthenticated gRPC call must not expose a security identity",
                "anonymous", reply.getMessage());
    }

    // -------------------------------------------------------------------------
    // Helper: HTTP BASIC call credentials
    // -------------------------------------------------------------------------

    private static CallCredentials basicAuth(String username, String password) {
        final String encoded = Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
        return new CallCredentials() {
            @Override
            public void applyRequestMetadata(RequestInfo requestInfo, Executor appExecutor,
                    MetadataApplier applier) {
                final Metadata metadata = new Metadata();
                metadata.put(
                        Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER),
                        "Basic " + encoded);
                applier.apply(metadata);
            }

            @Override
            public void thisUsesUnstableApi() {
            }
        };
    }
}

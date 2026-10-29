/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.feature.pack.grpc.test.security;

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
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.feature.pack.grpc.test.helloworld.GreeterGrpc;
import org.wildfly.feature.pack.grpc.test.utility.BasicCallCredentials;
import org.wildfly.feature.pack.grpc.test.utility.ServerReload;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import messages.HelloRequest;

/**
 * Verifies that role-based access control works for CDI beans called from gRPC service methods,
 * using the Elytron {@link org.wildfly.security.auth.server.SecurityIdentity} propagated by the
 * subsystem's built-in {@code ElytronSecurityIdentityInterceptor}.
 *
 * <p>The test configures an Elytron filesystem-realm with:
 * <ul>
 *   <li>Alice assigned the group {@code grpc-user} (mapped to a role via
 *       {@code simple-role-decoder}).</li>
 *   <li>A security domain wired into Undertow's {@code application-security-domain} so that
 *       {@code SecurityDomain.getCurrent()} is available inside the WAR handler chain.</li>
 * </ul>
 *
 * <p>The {@link RolesAllowedGreeterServiceImpl} gRPC service injects a CDI bean
 * ({@link RoleProtectedGreetingService}) whose {@code greet()} method performs an explicit
 * role check via {@link org.wildfly.extension.grpc.GrpcSecurity#SECURITY_IDENTITY_KEY}.
 * Access is granted only when the caller's identity carries the {@code grpc-user} role.
 */
@RunWith(Arquillian.class)
@ServerSetup(RolesAllowedSecurityIdentityTest.ElytronRolesSetupTask.class)
@RunAsClient
public class RolesAllowedSecurityIdentityTest {

    static final String REALM_NAME = "grpc-roles-test-realm";
    static final String DECODER_NAME = "grpc-roles-decoder";
    static final String DOMAIN_NAME = "grpc-roles-test-domain";
    static final String HTTP_AUTH_FACTORY_NAME = "grpc-roles-test-http-auth";
    static final String TEST_USER = "alice";
    static final String TEST_PASSWORD = "password";
    static final String TEST_ROLE = "grpc-user";

    private static ManagedChannel channel;
    private static GreeterGrpc.GreeterBlockingStub blockingStub;

    // -------------------------------------------------------------------------
    // Server setup
    // -------------------------------------------------------------------------

    public static class ElytronRolesSetupTask extends SnapshotServerSetupTask {

        @Override
        protected void doSetup(ManagementClient client, String containerId) throws Exception {
            final ModelNode realmAddress = Operations.createAddress(
                    "subsystem", "elytron", "filesystem-realm", REALM_NAME);

            final CompositeOperationBuilder builder = CompositeOperationBuilder.create();

            // 1. Filesystem-realm
            ModelNode op = Operations.createAddOperation(realmAddress);
            op.get("path").set(REALM_NAME);
            op.get("relative-to").set("jboss.server.data.dir");
            builder.addStep(op);

            // 2. Simple role-decoder: maps the identity's "groups" attribute to roles
            ModelNode decoderAddress = Operations.createAddress(
                    "subsystem", "elytron", "simple-role-decoder", DECODER_NAME);
            op = Operations.createAddOperation(decoderAddress);
            op.get("attribute").set("groups");
            builder.addStep(op);

            // 3. Security-domain with the role-decoder
            ModelNode domainAddress = Operations.createAddress(
                    "subsystem", "elytron", "security-domain", DOMAIN_NAME);
            op = Operations.createAddOperation(domainAddress);
            op.get("default-realm").set(REALM_NAME);
            op.get("permission-mapper").set("default-permission-mapper");
            // Wire the role-decoder into the realm entry so groups become roles
            final ModelNode realmEntry = op.get("realms").add();
            realmEntry.get("realm").set(REALM_NAME);
            realmEntry.get("role-decoder").set(DECODER_NAME);
            builder.addStep(op);

            // 4. HTTP authentication factory with BASIC mechanism
            ModelNode factoryAddress = Operations.createAddress(
                    "subsystem", "elytron", "http-authentication-factory", HTTP_AUTH_FACTORY_NAME);
            op = Operations.createAddOperation(factoryAddress);
            op.get("http-server-mechanism-factory").set("global");
            op.get("security-domain").set(DOMAIN_NAME);
            ModelNode mechConfig = op.get("mechanism-configurations").add();
            mechConfig.get("mechanism-name").set("BASIC");
            mechConfig.get("mechanism-realm-configurations").add().get("realm-name").set(REALM_NAME);
            builder.addStep(op);

            // 5. Undertow application-security-domain mapping.
            // override-deployment-config=true forces PRO_ACTIVE mode so credentials are
            // processed on every request, enabling SecurityDomain.getCurrent() in interceptCall().
            ModelNode appDomainAddress = Operations.createAddress(
                    "subsystem", "undertow", "application-security-domain", DOMAIN_NAME);
            op = Operations.createAddOperation(appDomainAddress);
            op.get("http-authentication-factory").set(HTTP_AUTH_FACTORY_NAME);
            op.get("override-deployment-config").set(true);
            builder.addStep(op);

            ModelNode compositeResult = client.getControllerClient().execute(builder.build());
            if (!Operations.isSuccessfulOutcome(compositeResult)) {
                throw new RuntimeException("Failed to set up Elytron roles config: "
                        + Operations.getFailureDescription(compositeResult));
            }

            // 6. Add alice with the grpc-user group (separate from composite to handle leftovers)
            ModelNode removeOp = Operations.createOperation("remove-identity", realmAddress);
            removeOp.get("identity").set(TEST_USER);
            client.getControllerClient().execute(removeOp);

            ModelNode addOp = Operations.createOperation("add-identity", realmAddress);
            addOp.get("identity").set(TEST_USER);
            client.getControllerClient().execute(addOp);

            ModelNode pwOp = Operations.createOperation("set-password", realmAddress);
            pwOp.get("identity").set(TEST_USER);
            pwOp.get("clear").get("password").set(TEST_PASSWORD);
            client.getControllerClient().execute(pwOp);

            // Assign alice the role group "grpc-user"
            ModelNode attrOp = Operations.createOperation("add-identity-attribute", realmAddress);
            attrOp.get("identity").set(TEST_USER);
            attrOp.get("name").set("groups");
            attrOp.get("value").add(TEST_ROLE);
            ModelNode attrResult = client.getControllerClient().execute(attrOp);
            if (!Operations.isSuccessfulOutcome(attrResult)) {
                throw new RuntimeException("Failed to add group to alice: "
                        + Operations.getFailureDescription(attrResult));
            }

            ServerReload.executeReloadAndWaitForCompletion(client.getControllerClient());
        }
    }

    // -------------------------------------------------------------------------
    // Deployment: CDI-enabled WAR with security domain
    // -------------------------------------------------------------------------

    // jboss-web.xml declares the security domain so WildFly activates the CDI security
    // interceptors that enforce @RolesAllowed for this deployment.
    private static final String JBOSS_WEB_XML = """
              <?xml version="1.0" encoding="UTF-8"?>
              <jboss-web>
                  <security-domain>%s</security-domain>
              </jboss-web>
              """.formatted(DOMAIN_NAME);

    // Minimal web.xml required so WildFly parses jboss-web.xml into WarMetaData.
    private static final String WEB_XML = """
              <?xml version="1.0" encoding="UTF-8"?>
              <web-app xmlns="https://jakarta.ee/xml/ns/jakartaee" version="6.0">
                  <login-config>
                      <auth-method>BASIC</auth-method>
                      <realm-name>%s</realm-name>
                  </login-config>
              </web-app>
              """.formatted(REALM_NAME);

    @Deployment
    public static Archive<?> createTestArchive() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "RolesAllowedSecurityIdentityTest.war");
        war.addClasses(RolesAllowedGreeterServiceImpl.class, RoleProtectedGreetingService.class);
        war.addPackage(HelloRequest.class.getPackage());
        war.addClass(GreeterGrpc.class);
        // beans.xml enables CDI — required for @Inject and @RolesAllowed CDI interceptor
        war.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        war.addAsWebInfResource(new StringAsset(WEB_XML), "web.xml");
        // jboss-web.xml declares the security domain so ElytronContextAssociationHandler activates
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

    /**
     * Authenticated user {@code alice} carries the {@code grpc-user} role.
     * The {@code @RolesAllowed("grpc-user")} check on the CDI bean must pass because
     * {@code ElytronSecurityIdentityInterceptor} wraps the service method with
     * {@code SecurityIdentity.runAs()}, making the identity ambient on the thread.
     */
    @Test
    public void authenticatedUserWithRoleCanCallProtectedMethod() {
        final String reply = blockingStub
                .withCallCredentials(new BasicCallCredentials(TEST_USER, TEST_PASSWORD))
                .sayHello(HelloRequest.newBuilder().setName("WildFly").build())
                .getMessage();
        Assert.assertEquals("Secured Hello WildFly", reply);
    }

    /**
     * An unauthenticated caller has no roles. The anonymous security identity is still set on
     * the thread via {@code runAs()} so the CDI security interceptor can evaluate
     * {@code @RolesAllowed} — and deny access because the anonymous identity has no roles.
     */
    @Test
    public void unauthenticatedCallerIsRefusedByRolesAllowed() {
        try {
            blockingStub.sayHello(HelloRequest.newBuilder().setName("WildFly").build());
            Assert.fail("Expected PERMISSION_DENIED from @RolesAllowed enforcement");
        } catch (StatusRuntimeException e) {
            Assert.assertEquals(io.grpc.Status.Code.PERMISSION_DENIED, e.getStatus().getCode());
        }
    }

}

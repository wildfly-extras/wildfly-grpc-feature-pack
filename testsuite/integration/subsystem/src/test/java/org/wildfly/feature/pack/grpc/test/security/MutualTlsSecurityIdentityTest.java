/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.feature.pack.grpc.test.security;

import java.io.InputStream;
import java.security.KeyStore;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManagerFactory;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.feature.pack.grpc.test.helloworld.GreeterGrpc;
import org.wildfly.feature.pack.grpc.test.helloworld.TwowaySecureHelloWorldTest;

import io.grpc.ChannelCredentials;
import io.grpc.Grpc;
import io.grpc.ManagedChannel;
import io.grpc.TlsChannelCredentials;
import messages.HelloReply;
import messages.HelloRequest;

/**
 * Verifies whether Undertow's mTLS listener-level authentication propagates an Elytron
 * {@link org.wildfly.security.auth.server.SecurityIdentity} to the gRPC
 * {@link io.grpc.Context} via the built-in {@code ElytronSecurityIdentityInterceptor}.
 *
 * <p>The test reuses the existing {@link TwowaySecureHelloWorldTest.MutualTlsServerSetupTask}
 * (which enables {@code need-client-auth=true} on the Undertow HTTPS listener) and makes a
 * two-way-TLS gRPC call. The service returns the principal name from
 * {@link org.wildfly.extension.grpc.GrpcSecurity#SECURITY_IDENTITY_KEY}, or
 * {@code "anonymous"} if the context key was not set.
 *
 * <p>This test answers the question: does mTLS at the Undertow listener level make
 * {@code SecurityDomain.getCurrent()} non-null by the time the gRPC interceptor runs?
 */
@RunWith(Arquillian.class)
@ServerSetup(TwowaySecureHelloWorldTest.MutualTlsServerSetupTask.class)
@RunAsClient
public class MutualTlsSecurityIdentityTest {

    private static final String TARGET_HOST = "127.0.0.1";
    private static final int SECURE_PORT = 8443;

    private static ManagedChannel channel;
    private static GreeterGrpc.GreeterBlockingStub blockingStub;

    @Deployment
    public static Archive<?> createTestArchive() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "MutualTlsSecurityIdentityTest.war");
        war.addClasses(SecurityIdentityGreeterServiceImpl.class);
        war.addPackage(HelloRequest.class.getPackage());
        war.addClass(GreeterGrpc.class);
        return war;
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        ClassLoader classLoader = MutualTlsSecurityIdentityTest.class.getClassLoader();

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
        blockingStub = GreeterGrpc.newBlockingStub(channel);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (channel != null) {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    /**
     * Verifies that mTLS at the Undertow listener level does NOT automatically propagate
     * a {@link org.wildfly.security.auth.server.SecurityIdentity} to the gRPC context
     * when the WAR has no security domain configured.
     *
     * <p>WildFly's mTLS setup ({@code need-client-auth=true}) validates the client
     * certificate at the TLS layer, but without a security domain declared in
     * {@code jboss-web.xml} and a matching {@code application-security-domain} mapping
     * in Undertow, {@code ElytronContextAssociationHandler} is not activated for the
     * deployment and the identity bridge in {@code GrpcServletInstallProcessor} skips
     * identity propagation. The result is an anonymous identity.
     *
     * <p>To propagate the client certificate identity, configure a security domain on
     * the WAR as demonstrated by {@link MutualTlsElytronSecurityIdentityTest}.
     */
    @Test
    public void securityIdentityIsAnonymousWithoutElytronConfig() {
        HelloRequest request = HelloRequest.newBuilder().setName("test").build();
        HelloReply reply = blockingStub.sayHello(request);

        Assert.assertEquals(
                "mTLS at the Undertow listener level should NOT propagate a SecurityIdentity " +
                "to the gRPC context without explicit elytron-security-domain configuration",
                "anonymous", reply.getMessage());
    }
}

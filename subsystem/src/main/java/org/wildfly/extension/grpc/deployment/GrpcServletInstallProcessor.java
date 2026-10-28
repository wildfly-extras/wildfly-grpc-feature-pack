/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.grpc.deployment;

import static org.wildfly.extension.grpc._private.GrpcLogger.LOGGER;

import java.net.InetSocketAddress;
import java.security.cert.X509Certificate;
import java.util.List;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.wildfly.extension.grpc.GrpcSecurity;
import org.wildfly.extension.undertow.deployment.UndertowAttachments;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityIdentity;
import org.wildfly.security.auth.server.ServerAuthenticationContext;

import io.grpc.BindableService;
import io.grpc.servlet.jakarta.GrpcServlet;
import io.grpc.servlet.jakarta.ServletServerBuilder;
import io.grpc.util.MutableHandlerRegistry;
import io.undertow.security.api.AuthenticationMode;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.util.ImmediateInstanceFactory;

/**
 * Deployment unit processor that embeds a {@link GrpcServlet} into each WAR that
 * contains gRPC services. Runs at {@code POST_MODULE} after {@link GrpcDeploymentProcessor}
 * has discovered the services.
 * <p>
 * Adding the servlet directly to the WAR's handler chain means WildFly's normal
 * deployment pipeline — including {@code ElytronContextAssociationHandler} — runs
 * for every gRPC request, making per-WAR security domains work without any custom
 * security infrastructure in the gRPC subsystem.
 */
public class GrpcServletInstallProcessor implements DeploymentUnitProcessor {

    private final int maxInboundMessageSize;
    private final int maxInboundMetadataSize;

    public GrpcServletInstallProcessor(int maxInboundMessageSize, int maxInboundMetadataSize) {
        this.maxInboundMessageSize = maxInboundMessageSize;
        this.maxInboundMetadataSize = maxInboundMetadataSize;
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final List<Class<? extends BindableService>> services = deploymentUnit
                .getAttachment(GrpcDeploymentAttachments.GRPC_BINDABLE_SERVICES);

        if (services == null || services.isEmpty()) {
            return;
        }

        // Note: UndertowAttachments.RESOLVED_SECURITY_DOMAIN may not be set yet at
        // POST_MODULE phase. We defer the security-domain check into the servlet
        // extension callback where DeploymentInfo is fully populated.

        final MutableHandlerRegistry registry = new MutableHandlerRegistry();
        final GrpcServlet grpcServlet = (GrpcServlet) new ServletServerBuilder()
                .maxInboundMessageSize(maxInboundMessageSize)
                .maxInboundMetadataSize(maxInboundMetadataSize)
                .fallbackHandlerRegistry(registry)
                .buildServlet();

        deploymentUnit.addToAttachmentList(UndertowAttachments.UNDERTOW_SERVLET_EXTENSIONS,
                (deploymentInfo, servletContext) -> {
                    deploymentInfo.addServlet(
                            Servlets.servlet("grpc", GrpcServlet.class, new ImmediateInstanceFactory<>(grpcServlet))
                                    .addMapping("/grpc-dispatch/*")
                                    .setAsyncSupported(true));

                    // Read RESOLVED_SECURITY_DOMAIN here (inside the servlet extension callback)
                    // rather than at POST_MODULE time, because it is set by WildFly's deployment
                    // processors that run before deploymentManager.deploy() is called (RUNTIME).
                    final boolean hasSecurityDomain =
                            deploymentUnit.getAttachment(UndertowAttachments.RESOLVED_SECURITY_DOMAIN) != null;

                    // Force PRO_ACTIVE authentication so ElytronContextAssociationHandler processes
                    // credentials on every request even without security constraints.
                    // Conditions: WAR must have application security domain AND not use CLIENT_CERT
                    // (CLIENT_CERT auth is TLS-level; forcing PRO_ACTIVE causes 403 challenges).
                    if (hasSecurityDomain) {
                        final io.undertow.servlet.api.LoginConfig lc = deploymentInfo.getLoginConfig();
                        final boolean isClientCert = lc != null && lc.getAuthMethods().stream()
                                .anyMatch(m -> "CLIENT_CERT".equalsIgnoreCase(m.getName()));
                        if (!isClientCert) {
                            deploymentInfo.setAuthenticationMode(AuthenticationMode.PRO_ACTIVE);
                        }
                    }

                    // Insert an identity-bridge handler at the FRONT of the inner handler chain list
                    // (index 0, innermost). ElytronRunAsHandler (appended later by
                    // AuthenticationManager.configure()) becomes the outermost inner handler and runs
                    // FIRST — calling alice.runAs() — then our bridge runs inside that scope.
                    // For unsecured WARs the bridge is a no-op; pass through directly.
                    deploymentInfo.getInnerHandlerChainWrappers().add(0, next -> exchange -> {
                        if (!hasSecurityDomain) {
                            next.handleRequest(exchange);
                            return;
                        }
                        final SecurityDomain domain = SecurityDomain.getCurrent();
                        SecurityIdentity identity =
                                domain != null ? domain.getCurrentSecurityIdentity() : null;

                        // For mTLS: ElytronContextAssociationHandler may run in CONSTRAINT_DRIVEN
                        // mode and not authenticate the client certificate. Extract the cert from
                        // the TLS session and authenticate it explicitly via the security domain.
                        if ((identity == null || identity.isAnonymous()) && domain != null) {
                            try {
                                final io.undertow.server.SSLSessionInfo sslInfo =
                                        exchange.getConnection().getSslSessionInfo();
                                if (sslInfo != null) {
                                    final java.security.cert.Certificate[] certs =
                                            sslInfo.getPeerCertificates();
                                    if (certs != null && certs.length > 0) {
                                        final X509Certificate x509 = (X509Certificate) certs[0];
                                        final ServerAuthenticationContext sac =
                                                domain.createNewAuthenticationContext();
                                        try {
                                            sac.setAuthenticationPrincipal(
                                                    x509.getSubjectX500Principal());
                                            if (sac.authorize()) {
                                                identity = sac.getAuthorizedIdentity();
                                            }
                                        } finally {
                                            sac.close();
                                        }
                                    }
                                }
                            } catch (Exception ignored) {
                                // SSL session or cert authentication not available
                            }
                        }

                        final InetSocketAddress remoteAddr = (InetSocketAddress)
                                exchange.getConnection().getPeerAddress(InetSocketAddress.class);
                        if (identity != null && remoteAddr != null) {
                            GrpcSecurity.AUTHENTICATED_IDENTITY_BY_REMOTE_ADDR.put(remoteAddr, identity);
                            // Clean up when the connection closes rather than when individual
                            // exchanges complete. Under HTTP/2 multiplexing, multiple streams
                            // share the same connection and remote address; removing on exchange
                            // completion would race with other streams' interceptor reads.
                            exchange.getConnection().addCloseListener(
                                    conn -> GrpcSecurity.AUTHENTICATED_IDENTITY_BY_REMOTE_ADDR
                                            .remove(remoteAddr));
                        }
                        next.handleRequest(exchange);
                    });
                });

        deploymentUnit.putAttachment(GrpcDeploymentAttachments.GRPC_HANDLER_REGISTRY, registry);
        LOGGER.debugf("Installed gRPC servlet in WAR %s", deploymentUnit.getName());
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }
}

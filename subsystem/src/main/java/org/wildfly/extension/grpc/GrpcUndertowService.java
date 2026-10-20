/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.grpc;

import static org.wildfly.extension.grpc._private.GrpcLogger.LOGGER;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import java.net.InetSocketAddress;
import java.security.Principal;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import org.wildfly.extension.undertow.Host;
import org.wildfly.extension.undertow.UndertowFilter;
import org.wildfly.extension.undertow.UndertowListener;
import org.wildfly.security.auth.server.HttpAuthenticationFactory;
import org.wildfly.security.auth.server.SecurityDomain;

import io.grpc.BindableService;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;
import io.grpc.servlet.jakarta.GrpcServlet;
import io.grpc.servlet.jakarta.ServletServerBuilder;
import io.grpc.util.MutableHandlerRegistry;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ServletContainer;
import io.undertow.servlet.util.ImmediateInstanceFactory;
import io.undertow.util.Headers;

/**
 * An MSC service that serves gRPC traffic via WildFly's Undertow HTTP/2 stack.
 * <p>
 * This service registers an {@link UndertowFilter} on the configured virtual host.
 * The filter inspects the {@code Content-Type} header and routes {@code application/grpc}
 * requests through a programmatic servlet deployment backed by {@code grpc-servlet-jakarta}.
 * All other traffic passes through the existing handler chain unmodified.
 */
class GrpcUndertowService implements Service, WildFlyGrpcDeploymentRegistry {

    private static final String GRPC_CONTENT_TYPE = "application/grpc";

    /**
     * gRPC {@link io.grpc.Context} key carrying the client certificate's {@link Principal}.
     * Unused — kept as a fallback for transports that do propagate {@code Context.current()}.
     */
    static final io.grpc.Context.Key<Principal> PEER_PRINCIPAL_KEY =
            io.grpc.Context.key("wildfly.grpc.peer-principal");

    /**
     * Maps each client's remote {@link InetSocketAddress} to the TLS peer certificate's
     * subject {@link Principal}. The filter populates this map when a client certificate is
     * present; the interceptor looks up by {@link io.grpc.Grpc#TRANSPORT_ATTR_REMOTE_ADDR}.
     * <p>
     * Entries persist for the lifetime of the TLS connection — all multiplexed gRPC calls on
     * the same connection share the same client certificate, so reuse is correct.
     */
    static final ConcurrentHashMap<InetSocketAddress, Principal> PEER_PRINCIPAL_BY_ADDRESS =
            new ConcurrentHashMap<>();

    private final Consumer<GrpcUndertowService> serviceConsumer;
    private final Supplier<Host> undertowHost;
    private final int maxInboundMessageSize;
    private final int maxInboundMetadataSize;
    private final Supplier<HttpAuthenticationFactory> httpAuthFactorySupplier;
    private final Map<String, String> serviceOwners;

    private volatile MutableHandlerRegistry registry;
    private volatile GrpcServlet grpcServlet;
    private volatile DeploymentManager deploymentManager;
    private volatile UndertowFilter grpcFilter;
    private volatile Host resolvedHost;

    GrpcUndertowService(final Consumer<GrpcUndertowService> serviceConsumer,
            final Supplier<Host> undertowHost,
            final int maxInboundMessageSize,
            final int maxInboundMetadataSize,
            final Supplier<HttpAuthenticationFactory> httpAuthFactorySupplier) {
        this.serviceConsumer = serviceConsumer;
        this.undertowHost = undertowHost;
        this.maxInboundMessageSize = maxInboundMessageSize;
        this.maxInboundMetadataSize = maxInboundMetadataSize;
        this.httpAuthFactorySupplier = httpAuthFactorySupplier;
        this.serviceOwners = new ConcurrentHashMap<>();
    }

    @Override
    public void start(final StartContext context) throws StartException {
        registry = new MutableHandlerRegistry();

        // ServletServerBuilder delegates transport concerns (keepalive, connection age/idle)
        // to the servlet container (Undertow). Only message-level settings are applicable.
        final ServletServerBuilder builder = new ServletServerBuilder()
                .fallbackHandlerRegistry(registry)
                .maxInboundMessageSize(maxInboundMessageSize)
                .maxInboundMetadataSize(maxInboundMetadataSize);

        grpcServlet = builder.buildServlet();

        // Create a standalone servlet container (independent of WildFly's default container)
        // so this deployment doesn't interfere with application deployments.
        ServletContainer servletContainer = Servlets.newContainer();
        final DeploymentInfo deploymentInfo = Servlets.deployment()
                .setClassLoader(GrpcUndertowService.class.getClassLoader())
                .setContextPath("/")
                .setDeploymentName("wildfly-grpc-subsystem")
                .addServlet(Servlets.servlet("GrpcServlet", GrpcServlet.class, new ImmediateInstanceFactory<>(grpcServlet))
                        .addMapping("/*").setAsyncSupported(true));

        deploymentManager = servletContainer.addDeployment(deploymentInfo);
        final HttpHandler servletHandler;
        try {
            deploymentManager.deploy();
            servletHandler = deploymentManager.start();
        } catch (Exception e) {
            try {
                deploymentManager.undeploy();
            } catch (Exception suppressed) {
                e.addSuppressed(suppressed);
            }
            deploymentManager = null;
            throw new StartException(e);
        }

        // Register an UndertowFilter that wraps the host's root handler.
        // This sits above Undertow's path router so application/grpc requests are
        // intercepted before any context-path dispatch — including ROOT.war deployments
        // at "/". All other traffic passes through to the existing handler chain unchanged.
        grpcFilter = new UndertowFilter() {
            @Override
            public int getPriority() {
                return 1;
            }

            @Override
            public HttpHandler wrap(final HttpHandler next) {
                return Handlers.predicate(
                        exchange -> {
                            final String ct = exchange.getRequestHeaders().getFirst(Headers.CONTENT_TYPE);
                            // gRPC content-type is "application/grpc[+<sub-type>]".
                            // Accepts bare "application/grpc" and sub-types like +proto, +json.
                            // Correctly excludes "application/grpc-web" and other non-gRPC types.
                            return ct != null
                                    && ct.regionMatches(true, 0, GRPC_CONTENT_TYPE, 0, GRPC_CONTENT_TYPE.length())
                                    && (ct.length() == GRPC_CONTENT_TYPE.length()
                                            || ct.charAt(GRPC_CONTENT_TYPE.length()) == '+');
                        },
                        // When http-authentication-factory is configured, wrap the servlet handler
                        // to propagate the TLS peer principal via gRPC Context before the RPC call
                        // is dispatched to the gRPC executor. gRPC captures Context.current() when
                        // submitting the call task, so values attached here are visible to interceptors.
                        httpAuthFactorySupplier != null
                                ? peerPrincipalAttachingHandler(servletHandler)
                                : servletHandler,
                        next);
            }
        };
        resolvedHost = undertowHost.get();
        resolvedHost.addFilter(grpcFilter);

        // When an http-authentication-factory is configured, register the subsystem ClassLoader
        // with its security domain. This makes SecurityDomain.getCurrent() return the domain
        // on any gRPC executor thread whose context ClassLoader is set by SUBSYSTEM_CLASS_LOADER_INTERCEPTOR,
        // allowing ElytronSecurityIdentityInterceptor to authenticate from gRPC call metadata.
        if (httpAuthFactorySupplier != null) {
            httpAuthFactorySupplier.get().getSecurityDomain()
                    .registerWithClassLoader(GrpcUndertowService.class.getClassLoader());
        }

        LOGGER.grpcServingViaUndertow(resolvedHost.getName());
        serviceConsumer.accept(this);
    }

    @Override
    public void stop(final StopContext context) {
        LOGGER.grpcStopping();
        final Host host = resolvedHost;
        if (host != null && grpcFilter != null) {
            host.removeFilter(grpcFilter);
        }
        grpcFilter = null;
        resolvedHost = null;

        if (deploymentManager != null) {
            try {
                deploymentManager.stop();
            } catch (Exception e) {
                LOGGER.failedToStopGrpcServer(e);
            }
            try {
                deploymentManager.undeploy();
            } catch (Exception e) {
                LOGGER.failedToUndeployGrpcServer(e);
            }
            deploymentManager = null;
        }

        // grpcServlet.destroy() is called by deploymentManager.stop() via the servlet lifecycle
        grpcServlet = null;

        if (httpAuthFactorySupplier != null) {
            SecurityDomain.unregisterClassLoader(GrpcUndertowService.class.getClassLoader());
        }

        // Nullify the MSC service value first so no new addService() calls can arrive
        // before registry is cleared. Any concurrent caller that passes the supplier check
        // after this point will see a null registry in addService() and NPE — but MSC
        // guarantees no dependent services start after serviceConsumer.accept(null).
        serviceConsumer.accept(null);
        registry = null;
    }

    @Override
    public ServerServiceDefinition addService(final String deploymentName, final BindableService service,
            final List<ServerInterceptor> interceptors) throws StartException {
        // Prepend built-in interceptors outermost (before any user-provided interceptors).
        // When http-authentication-factory is configured, the SubsystemClassLoaderInterceptor
        // sets the thread context ClassLoader to the subsystem ClassLoader (which is registered
        // with the security domain) so that ElytronSecurityIdentityInterceptor can call
        // SecurityDomain.getCurrent() and authenticate from gRPC call metadata.
        final List<ServerInterceptor> all = new ArrayList<>(interceptors.size() + 2);
        if (httpAuthFactorySupplier != null) {
            all.add(SUBSYSTEM_CLASS_LOADER_INTERCEPTOR);
        }
        all.add(ElytronSecurityIdentityInterceptor.INSTANCE);
        all.addAll(interceptors);
        final ServerServiceDefinition ssd = ServerInterceptors.intercept(service, all);
        final String serviceName = ssd.getServiceDescriptor().getName();
        final String existingOwner = serviceOwners.get(serviceName);
        if (existingOwner != null && !existingOwner.equals(deploymentName)) {
            throw LOGGER.duplicateGrpcServiceName(serviceName, existingOwner);
        }
        registry.addService(ssd);
        serviceOwners.put(serviceName, deploymentName);
        return ssd;
    }

    @Override
    public boolean isHttp2Configured() {
        final Host host = resolvedHost;
        if (host == null) {
            return true; // service not yet started; skip warning
        }
        // HTTPS listeners always support h2 via ALPN.
        // Plain HTTP listeners need enable-http2="true" for h2c, but there is no public API
        // to inspect that option — only secure listeners can be reliably detected here.
        return host.getServer().getListeners().stream()
                .anyMatch(UndertowListener::isSecure);
    }

    /**
     * Wraps the servlet handler to attach the mTLS peer principal to the gRPC {@link io.grpc.Context}
     * before the RPC call is submitted to the gRPC executor. gRPC captures {@link io.grpc.Context#current()}
     * when it enqueues the call task, so the principal becomes visible to interceptors running on
     * the gRPC executor thread via {@link #PEER_PRINCIPAL_KEY}.
     */
    /**
     * Wraps the servlet handler to store the mTLS peer principal in {@link #PEER_PRINCIPAL_BY_ADDRESS}
     * keyed by the client's remote address. gRPC's {@code ServletAdapter} sets the same address as
     * {@link io.grpc.Grpc#TRANSPORT_ATTR_REMOTE_ADDR} on the {@code ServerCall}, so the interceptor
     * can look it up without crossing thread boundaries.
     */
    private static HttpHandler peerPrincipalAttachingHandler(final HttpHandler next) {
        return exchange -> {
            final Principal peerPrincipal = extractPeerPrincipal(exchange);
            if (peerPrincipal != null) {
                final InetSocketAddress remoteAddr =
                        (InetSocketAddress) exchange.getConnection().getPeerAddress(InetSocketAddress.class);
                if (remoteAddr != null) {
                    PEER_PRINCIPAL_BY_ADDRESS.put(remoteAddr, peerPrincipal);
                }
            }
            next.handleRequest(exchange);
        };
    }

    private static Principal extractPeerPrincipal(HttpServerExchange exchange) {
        try {
            final io.undertow.server.SSLSessionInfo sslInfo =
                    exchange.getConnection().getSslSessionInfo();
            if (sslInfo == null) {
                return null;
            }
            final Certificate[] certs = sslInfo.getPeerCertificates();
            if (certs == null || certs.length == 0) {
                return null;
            }
            return ((X509Certificate) certs[0]).getSubjectX500Principal();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Singleton interceptor that sets the thread context ClassLoader to the gRPC subsystem's
     * own ClassLoader during each call setup. When {@code http-authentication-factory} is
     * configured, the subsystem ClassLoader is registered with the security domain at startup,
     * so switching to it here makes {@link SecurityDomain#getCurrent()} return the domain
     * for the {@link ElytronSecurityIdentityInterceptor} to authenticate against.
     */
    private static final io.grpc.ServerInterceptor SUBSYSTEM_CLASS_LOADER_INTERCEPTOR =
            new io.grpc.ServerInterceptor() {
        private final ClassLoader subsystemClassLoader = GrpcUndertowService.class.getClassLoader();

        @Override
        public <ReqT, RespT> io.grpc.ServerCall.Listener<ReqT> interceptCall(
                io.grpc.ServerCall<ReqT, RespT> call, io.grpc.Metadata headers,
                io.grpc.ServerCallHandler<ReqT, RespT> next) {
            // Set the subsystem ClassLoader during startCall() so ElytronSecurityIdentityInterceptor
            // can call SecurityDomain.getCurrent() in its interceptCall() method.
            final Thread thread = Thread.currentThread();
            final ClassLoader previous = thread.getContextClassLoader();
            thread.setContextClassLoader(subsystemClassLoader);
            try {
                final io.grpc.ServerCall.Listener<ReqT> delegate = next.startCall(call, headers);
                // Also switch the ClassLoader for every listener callback so that
                // SecurityDomain.getCurrent() works within the service method thread too,
                // which is required for SecurityIdentity.runAs() called by the inner interceptor.
                return new io.grpc.ForwardingServerCallListener
                        .SimpleForwardingServerCallListener<>(delegate) {
                    @Override public void onMessage(ReqT message) {
                        withCL(() -> super.onMessage(message));
                    }
                    @Override public void onHalfClose() {
                        withCL(super::onHalfClose);
                    }
                    @Override public void onCancel() {
                        withCL(super::onCancel);
                    }
                    @Override public void onComplete() {
                        withCL(super::onComplete);
                    }
                    @Override public void onReady() {
                        withCL(super::onReady);
                    }
                    private void withCL(Runnable action) {
                        final Thread t = Thread.currentThread();
                        final ClassLoader prev = t.getContextClassLoader();
                        t.setContextClassLoader(subsystemClassLoader);
                        try {
                            action.run();
                        } finally {
                            t.setContextClassLoader(prev);
                        }
                    }
                };
            } finally {
                thread.setContextClassLoader(previous);
            }
        }
    };

    @Override
    public void removeService(final ServerServiceDefinition ssd) {
        // registry is null when the gRPC server has already stopped (e.g. during server shutdown).
        // In that case services are already gone, so there is nothing to remove.
        final MutableHandlerRegistry r = registry;
        if (r != null) {
            r.removeService(ssd);
            serviceOwners.remove(ssd.getServiceDescriptor().getName());
        }
    }
}

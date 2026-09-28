/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.grpc;

import static org.wildfly.extension.grpc._private.GrpcLogger.LOGGER;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.undertow.Host;
import org.wildfly.extension.undertow.UndertowFilter;

import io.grpc.BindableService;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;
import io.grpc.servlet.jakarta.GrpcServlet;
import io.grpc.servlet.jakarta.ServletServerBuilder;
import io.grpc.util.MutableHandlerRegistry;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
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

    private final Consumer<GrpcUndertowService> serviceConsumer;
    private final Supplier<Host> undertowHost;
    private final ServletConfiguration configuration;
    private final Map<String, String> serviceOwners;

    private volatile MutableHandlerRegistry registry;
    private volatile GrpcServlet grpcServlet;
    private volatile DeploymentManager deploymentManager;
    private volatile UndertowFilter grpcFilter;
    private volatile Host resolvedHost;

    GrpcUndertowService(final Consumer<GrpcUndertowService> serviceConsumer,
            final Supplier<Host> undertowHost,
            final ServletConfiguration configuration) {
        this.serviceConsumer = serviceConsumer;
        this.undertowHost = undertowHost;
        this.configuration = configuration;
        this.serviceOwners = new ConcurrentHashMap<>();
    }

    @Override
    public void start(final StartContext context) throws StartException {
        registry = new MutableHandlerRegistry();

        // ServletServerBuilder delegates transport concerns (keepalive, connection age/idle)
        // to the servlet container (Undertow). Only message-level settings are applicable.
        final ServletServerBuilder builder = new ServletServerBuilder()
                .fallbackHandlerRegistry(registry)
                .maxInboundMessageSize(configuration.maxInboundMessageSize())
                .maxInboundMetadataSize(configuration.maxInboundMetadataSize());

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
                        servletHandler, next);
            }
        };
        resolvedHost = undertowHost.get();
        resolvedHost.addFilter(grpcFilter);

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
        final ServerServiceDefinition ssd = ServerInterceptors.intercept(service, interceptors);
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

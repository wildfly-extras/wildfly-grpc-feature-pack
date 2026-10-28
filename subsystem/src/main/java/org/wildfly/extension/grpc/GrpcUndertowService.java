/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.grpc;

import static org.wildfly.extension.grpc._private.GrpcLogger.LOGGER;

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
import org.wildfly.extension.undertow.UndertowListener;

import io.undertow.server.HttpHandler;
import io.undertow.util.Headers;

/**
 * MSC service that routes incoming gRPC traffic to the correct WAR deployment.
 * <p>
 * A single {@link UndertowFilter} is registered on the configured virtual host.
 * For each {@code application/grpc} request, the filter extracts the gRPC service
 * name from the request path and rewrites the relative path to include the owning
 * WAR's context path, allowing Undertow's normal path-based routing (including the
 * {@code ElytronContextAssociationHandler}) to handle the request as if it had
 * arrived at the WAR's own context path.
 */
class GrpcUndertowService implements Service, WildFlyGrpcDeploymentRegistry {

    private static final String GRPC_CONTENT_TYPE = "application/grpc";

    private final Consumer<GrpcUndertowService> serviceConsumer;
    private final Supplier<Host> undertowHost;
    private final int maxInboundMessageSize;
    private final int maxInboundMetadataSize;

    /** Maps gRPC service name (e.g. "helloworld.Greeter") → WAR context path (e.g. "/my-app"). */
    private final Map<String, String> serviceContextPaths = new ConcurrentHashMap<>();

    private volatile UndertowFilter grpcFilter;
    private volatile Host resolvedHost;

    GrpcUndertowService(final Consumer<GrpcUndertowService> serviceConsumer,
            final Supplier<Host> undertowHost,
            final int maxInboundMessageSize,
            final int maxInboundMetadataSize) {
        this.serviceConsumer = serviceConsumer;
        this.undertowHost = undertowHost;
        this.maxInboundMessageSize = maxInboundMessageSize;
        this.maxInboundMetadataSize = maxInboundMetadataSize;
    }

    @Override
    public void start(final StartContext context) throws StartException {
        grpcFilter = new UndertowFilter() {
            @Override
            public int getPriority() {
                return 1;
            }

            @Override
            public HttpHandler wrap(final HttpHandler next) {
                return exchange -> {
                    final String ct = exchange.getRequestHeaders().getFirst(Headers.CONTENT_TYPE);
                    if (ct != null
                            && ct.regionMatches(true, 0, GRPC_CONTENT_TYPE, 0, GRPC_CONTENT_TYPE.length())
                            && (ct.length() == GRPC_CONTENT_TYPE.length()
                                    || ct.charAt(GRPC_CONTENT_TYPE.length()) == '+')) {
                        final String relative = exchange.getRelativePath();
                        final int slash = relative.indexOf('/', 1);
                        final String serviceName = slash > 0
                                ? relative.substring(1, slash)
                                : relative.substring(1);
                        final String contextPath = serviceContextPaths.get(serviceName);
                        if (contextPath != null) {
                            // Route through the WAR's handler chain by rewriting the relative
                            // path. The /grpc-dispatch prefix targets the GrpcServlet (mapped
                            // at /grpc-dispatch/*) without conflicting with other /* mappings
                            // such as JAX-RS. ElytronContextAssociationHandler runs for all
                            // requests through the WAR, providing per-WAR security automatically.
                            final String prefix = "/".equals(contextPath) ? "" : contextPath;
                            exchange.setRelativePath(prefix + "/grpc-dispatch" + relative);
                        }
                    }
                    next.handleRequest(exchange);
                };
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
        serviceConsumer.accept(null);
    }

    @Override
    public void registerService(final String serviceName, final String contextPath) throws StartException {
        final String existingContextPath = serviceContextPaths.get(serviceName);
        if (existingContextPath != null && !existingContextPath.equals(contextPath)) {
            throw LOGGER.duplicateGrpcServiceName(serviceName, existingContextPath);
        }
        serviceContextPaths.put(serviceName, contextPath);
    }

    @Override
    public void unregisterService(final String serviceName) {
        serviceContextPaths.remove(serviceName);
    }

    @Override
    public boolean isHttp2Configured() {
        final Host host = resolvedHost;
        if (host == null) {
            return true;
        }
        return host.getServer().getListeners().stream()
                .anyMatch(UndertowListener::isSecure);
    }
}

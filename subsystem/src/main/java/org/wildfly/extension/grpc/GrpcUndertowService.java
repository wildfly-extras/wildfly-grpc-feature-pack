/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.grpc;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

import jakarta.servlet.ServletException;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.grpc._private.GrpcLogger;
import org.wildfly.extension.undertow.Host;
import org.wildfly.extension.undertow.UndertowFilter;

import io.grpc.BindableService;
import io.grpc.InternalServerInterceptors;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.ServerMethodDefinition;
import io.grpc.ServerServiceDefinition;
import io.grpc.servlet.jakarta.GrpcServlet;
import io.grpc.servlet.jakarta.ServletServerBuilder;
import io.grpc.util.MutableHandlerRegistry;
import io.undertow.server.HttpHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.InstanceHandle;
import io.undertow.servlet.api.ServletContainer;

/**
 * An MSC service that serves gRPC traffic via WildFly's Undertow HTTP/2 stack.
 * <p>
 * Rather than opening a separate Netty TCP socket, this service registers an
 * {@link UndertowFilter} on the configured virtual host. The filter inspects the
 * {@code Content-Type} header and routes {@code application/grpc} requests through a
 * programmatic servlet deployment backed by {@code grpc-servlet-jakarta}. All other
 * traffic passes through the existing handler chain unmodified.
 */
class GrpcUndertowService implements Service, WildFlyGrpcDeploymentRegistry {

    private final Consumer<GrpcUndertowService> serviceConsumer;
    private final Supplier<Host> undertowHost;
    private final ServerConfiguration configuration;
    private final Map<String, Collection<ServerServiceDefinition>> deploymentServices;

    private volatile MutableHandlerRegistry registry;
    private volatile GrpcServlet grpcServlet;
    private volatile DeploymentManager deploymentManager;
    private volatile ServletContainer servletContainer;
    private volatile UndertowFilter grpcFilter;

    GrpcUndertowService(final Consumer<GrpcUndertowService> serviceConsumer,
            final Supplier<Host> undertowHost,
            final ServerConfiguration configuration) {
        this.serviceConsumer = serviceConsumer;
        this.undertowHost = undertowHost;
        this.configuration = configuration;
        this.deploymentServices = new ConcurrentHashMap<>();
    }

    @Override
    public void start(final StartContext context) throws StartException {
        registry = new MutableHandlerRegistry();

        // ServletServerBuilder delegates transport concerns (keepalive, connection age/idle)
        // to the servlet container (Undertow). Only message-level settings are applicable.
        final ServletServerBuilder builder = new ServletServerBuilder()
                .fallbackHandlerRegistry(registry)
                .maxInboundMessageSize(configuration.getMaxInboundMessageSize())
                .maxInboundMetadataSize(configuration.getMaxInboundMetadataSize());

        grpcServlet = builder.buildServlet();

        // Create a standalone servlet container (independent of WildFly's default container)
        // so this deployment doesn't interfere with application deployments.
        servletContainer = Servlets.newContainer();
        final GrpcServlet servlet = grpcServlet;
        final io.undertow.servlet.api.InstanceFactory<GrpcServlet> factory = () -> new InstanceHandle<GrpcServlet>() {
            @Override
            public GrpcServlet getInstance() {
                return servlet;
            }

            @Override
            public void release() {
            }
        };
        final DeploymentInfo deploymentInfo = Servlets.deployment()
                .setClassLoader(GrpcUndertowService.class.getClassLoader())
                .setContextPath("/")
                .setDeploymentName("wildfly-grpc-subsystem")
                .addServlet(Servlets.servlet("GrpcServlet", GrpcServlet.class, factory)
                        .addMapping("/*").setAsyncSupported(true));

        deploymentManager = servletContainer.addDeployment(deploymentInfo);
        deploymentManager.deploy();

        final HttpHandler servletHandler;
        try {
            servletHandler = deploymentManager.start();
        } catch (ServletException e) {
            throw new StartException(e);
        }

        // Register an UndertowFilter that wraps the host's root handler.
        // This sits above Undertow's path router so application/grpc requests are
        // intercepted before any context-path dispatch — including ROOT.war deployments
        // at "/". All other traffic passes through to the existing handler chain unchanged.
        final HttpHandler servletHandlerRef = servletHandler;
        grpcFilter = new UndertowFilter() {
            @Override
            public int getPriority() {
                return 1;
            }

            @Override
            public HttpHandler wrap(final HttpHandler next) {
                return new GrpcRoutingHandler(servletHandlerRef, next);
            }
        };
        final Host host = undertowHost.get();
        host.addFilter(grpcFilter);

        GrpcLogger.LOGGER.grpcServingViaUndertow(host.getName());
        serviceConsumer.accept(this);
    }

    @Override
    public void stop(final StopContext context) {
        GrpcLogger.LOGGER.grpcStopping();
        final Host host = undertowHost.get();
        if (host != null && grpcFilter != null) {
            host.removeFilter(grpcFilter);
        }
        grpcFilter = null;

        if (deploymentManager != null) {
            try {
                deploymentManager.stop();
                deploymentManager.undeploy();
            } catch (Exception e) {
                GrpcLogger.LOGGER.failedToStopGrpcServer(e);
            }
            deploymentManager = null;
        }

        if (grpcServlet != null) {
            grpcServlet.destroy();
            grpcServlet = null;
        }

        serviceConsumer.accept(null);
    }

    @Override
    public void addService(final DeploymentUnit deployment, final Class<? extends BindableService> serviceType,
            final List<ServerInterceptor> interceptors) {
        final String deploymentName = deployment.getName();
        GrpcLogger.LOGGER.registerService(serviceType.getName(), deploymentName);
        final BindableService bindableService;
        if (System.getSecurityManager() == null) {
            try {
                final Constructor<? extends BindableService> constructor = serviceType.getConstructor();
                bindableService = constructor.newInstance();
            } catch (NoSuchMethodException | InvocationTargetException | InstantiationException
                    | IllegalAccessException e) {
                throw GrpcLogger.LOGGER.failedToRegister(e, serviceType.getName(), deploymentName);
            }
        } else {
            bindableService = AccessController.doPrivileged((PrivilegedAction<BindableService>) () -> {
                try {
                    final Constructor<? extends BindableService> constructor = serviceType.getConstructor();
                    return constructor.newInstance();
                } catch (NoSuchMethodException | InvocationTargetException | InstantiationException
                        | IllegalAccessException e) {
                    throw GrpcLogger.LOGGER.failedToRegister(e, serviceType.getName(), deploymentName);
                }
            });
        }
        final ServerServiceDefinition ssd = installInterceptors(bindableService.bindService(), interceptors)
                .bindService();
        deploymentServices.computeIfAbsent(deploymentName, k -> ConcurrentHashMap.newKeySet()).add(ssd);
        registry.addService(ssd);
    }

    @Override
    public void removeDeploymentServices(final DeploymentUnit deployment) {
        final Collection<ServerServiceDefinition> defs = deploymentServices.remove(deployment.getName());
        if (defs != null) {
            for (ServerServiceDefinition def : defs) {
                registry.removeService(def);
            }
        }
    }

    private static BindableService installInterceptors(final ServerServiceDefinition ssd,
            final List<ServerInterceptor> interceptors) {
        final ServerServiceDefinition.Builder builder = ServerServiceDefinition.builder(ssd.getServiceDescriptor());
        for (ServerMethodDefinition<?, ?> smd : ssd.getMethods()) {
            builder.addMethod(wrapMethod(smd, interceptors));
        }
        return new BindableService() {
            public ServerServiceDefinition bindService() {
                return builder.build();
            }
        };
    }

    private static <ReqT, RespT> ServerMethodDefinition<?, ?> wrapMethod(
            final ServerMethodDefinition<ReqT, RespT> method,
            final List<ServerInterceptor> interceptors) {
        ServerCallHandler<ReqT, RespT> handler = method.getServerCallHandler();
        for (ServerInterceptor interceptor : interceptors) {
            handler = InternalServerInterceptors.interceptCallHandlerCreate(interceptor, handler);
        }
        return method.withServerCallHandler(handler);
    }
}

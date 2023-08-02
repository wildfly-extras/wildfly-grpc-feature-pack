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
package org.wildfly.extension.grpc;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.grpc._private.GrpcLogger;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerServiceDefinition;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.util.MutableHandlerRegistry;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * A gRPC Server service.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class GrpcServerService implements Service, WildFlyGrpcDeploymentRegistry {
    private final Consumer<GrpcServerService> serverService;
    private final Supplier<ExecutorService> executorService;
    private final MutableHandlerRegistry registry;
    private final NettyServerBuilder serverBuilder;

    private final ServerConfiguration configuration;
    private final Map<String, Collection<ServerServiceDefinition>> deploymentServices;
    private volatile Server server;

    GrpcServerService(final NettyServerBuilder serverBuilder, final MutableHandlerRegistry registry,
            final Consumer<GrpcServerService> serverService, final Supplier<ExecutorService> executorService,
            final ServerConfiguration configuration) {
        this.serverBuilder = serverBuilder;
        this.registry = registry;
        this.serverService = serverService;
        this.executorService = executorService;
        this.configuration = configuration;
        deploymentServices = new ConcurrentHashMap<>();
    }

    @Override
    public void start(StartContext context) {
        context.asynchronous();
        executorService.get().submit(() -> {
            try {
                if (configuration.getKeyManager() != null) {
                    final SSLContext sslContext = configuration.getSslContext() == null ? null
                            : configuration.getSslContext()
                                    .get();
                    serverBuilder.sslContext(createSslContext(sslContext));
                }
                server = serverBuilder.build().start();
                GrpcLogger.LOGGER.serverListening(configuration.getHostName(), server.getPort());
                serverService.accept(this);
                context.complete();
            } catch (Throwable e) {
                context.failed(new StartException(e));
            }
        });
    }

    @Override
    public void stop(final StopContext context) {
        GrpcLogger.LOGGER.grpcStopping();
        final Server server = this.server;
        if (server != null) {
            try {
                server.shutdown().awaitTermination(configuration.getShutdownTimeout(), SECONDS);
            } catch (InterruptedException e) {
                GrpcLogger.LOGGER.failedToStopGrpcServer(e);
            }
        }
        serverService.accept(null);
    }

    @Override
    public void addService(final DeploymentUnit deployment, final Class<? extends BindableService> serviceType) {
        final String deploymentName = deployment.getName();
        GrpcLogger.LOGGER.registerService(serviceType.getName(), deploymentName);
        // We must have a no-arg constructor
        final BindableService bindableService;
        if (System.getSecurityManager() == null) {
            try {
                final Constructor<? extends BindableService> constructor = serviceType.getConstructor();
                bindableService = constructor.newInstance();
            } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
                throw GrpcLogger.LOGGER.failedToRegisterService(e, serviceType.getName(), deploymentName);
            }
        } else {
            bindableService = AccessController.doPrivileged((PrivilegedAction<BindableService>) () -> {
                try {
                    final Constructor<? extends BindableService> constructor = serviceType.getConstructor();
                    return constructor.newInstance();
                } catch (NoSuchMethodException | InvocationTargetException | InstantiationException
                        | IllegalAccessException e) {
                    throw GrpcLogger.LOGGER.failedToRegisterService(e, serviceType.getName(), deploymentName);
                }
            });
        }
        final Collection<ServerServiceDefinition> defs = deploymentServices.computeIfAbsent(deploymentName,
                (c) -> new ArrayList<>());
        defs.add(registry.addService(bindableService));
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

    private SslContext createSslContext(final SSLContext sslContext) throws SSLException {
        final SslContextBuilder sslContextBuilder = SslContextBuilder.forServer(configuration.getKeyManager().get());
        if (sslContext != null) {
            sslContextBuilder.sslContextProvider(sslContext.getProvider());
            final SSLEngine sslEngine = sslContext.createSSLEngine();
            sslContextBuilder.ciphers(Arrays.asList(sslEngine.getEnabledCipherSuites()));
            sslContextBuilder.protocols(sslContext.getDefaultSSLParameters().getApplicationProtocols());
            if (configuration.getTrustManager() != null) {
                sslContextBuilder.trustManager(configuration.getTrustManager().get());
            }
        }
        if (configuration.getProtocolProvider() != null) {
            sslContextBuilder.sslProvider(SslProvider.valueOf(configuration.getProtocolProvider()));
        }
        if (configuration.getSessionCacheSize() != null) {
            sslContextBuilder.sessionCacheSize(configuration.getSessionCacheSize());
        }
        if (configuration.getSessionTimeout() != null) {
            sslContextBuilder.sessionTimeout(configuration.getSessionTimeout());
        }
        sslContextBuilder.startTls(configuration.isStartTls());
        return GrpcSslContexts.configure(sslContextBuilder).build();
    }
}

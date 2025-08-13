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
import java.net.InetSocketAddress;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
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
import io.grpc.InternalServerInterceptors;
import io.grpc.Server;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.ServerMethodDefinition;
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

    private final ServerConfiguration configuration;
    private final Map<String, Collection<ServerServiceDefinition>> deploymentServices;

    private volatile MutableHandlerRegistry registry;
    private volatile Server server;

    GrpcServerService(final Consumer<GrpcServerService> serverService, final Supplier<ExecutorService> executorService,
            final ServerConfiguration configuration) {
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
                registry = new MutableHandlerRegistry();
                NettyServerBuilder serverBuilder = NettyServerBuilder
                        .forAddress(new InetSocketAddress(configuration.getHostName(), configuration.getServerPort()));
                serverBuilder.fallbackHandlerRegistry(registry);
                serverBuilder.flowControlWindow(configuration.getFlowControlWindow());
                serverBuilder.handshakeTimeout(configuration.getHandshakeTimeout(), TimeUnit.SECONDS);
                serverBuilder.initialFlowControlWindow(configuration.getInitialFlowControlWindow());
                if (configuration.getKeepLiveTime() > 0) {
                    serverBuilder.keepAliveTime(configuration.getKeepLiveTime(), TimeUnit.SECONDS);
                }
                if (configuration.getKeepAliveTimeout() > 0) {
                    serverBuilder.keepAliveTimeout(configuration.getKeepAliveTimeout(), TimeUnit.SECONDS);
                }
                if (configuration.getMaxConcurrentCallsPerConnection() > 0) {
                    serverBuilder.maxConcurrentCallsPerConnection(configuration.getMaxConcurrentCallsPerConnection());
                }
                if (configuration.getMaxConnectionAge() > 0) {
                    serverBuilder.maxConnectionAge(configuration.getMaxConnectionAge(), TimeUnit.SECONDS);
                }
                if (configuration.getMaxConnectionAgeGrace() > 0) {
                    serverBuilder.maxConnectionAgeGrace(configuration.getMaxConnectionAgeGrace(), TimeUnit.SECONDS);
                }
                if (configuration.getMaxConnectionIdle() > 0) {
                    serverBuilder.maxConnectionIdle(configuration.getMaxConnectionIdle(), TimeUnit.SECONDS);
                }
                serverBuilder.maxInboundMessageSize(configuration.getMaxInboundMessageSize());
                serverBuilder.maxInboundMetadataSize(configuration.getMaxInboundMetadataSize());
                if (configuration.getPermitKeepAliveTime() > 0) {
                    serverBuilder.permitKeepAliveTime(configuration.getPermitKeepAliveTime(), TimeUnit.SECONDS);
                }
                serverBuilder.permitKeepAliveWithoutCalls(configuration.isPermitKeepAliveWithoutCalls());

                if (configuration.getKeyManager() != null) {
                    final SSLContext sslContext = configuration.getSslContext() == null ? null
                            : configuration.getSslContext().get();
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
    public void addService(final DeploymentUnit deployment, final Class<? extends BindableService> serviceType,
            List<ServerInterceptor> interceptors) {
        final String deploymentName = deployment.getName();
        GrpcLogger.LOGGER.registerService(serviceType.getName(), deploymentName);
        // We must have a no-arg constructor
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
        registry.addService(installInterceptors(bindableService.bindService(), interceptors));
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

    private static BindableService installInterceptors(ServerServiceDefinition ssd,
            List<ServerInterceptor> interceptors) {
        ServerServiceDefinition.Builder builder = ServerServiceDefinition.builder(ssd.getServiceDescriptor());
        for (ServerMethodDefinition<?, ?> smd : ssd.getMethods()) {
            builder.addMethod(wrapMethod(smd, interceptors));
        }
        return new BindableService() {
            public ServerServiceDefinition bindService() {
                return builder.build();
            }
        };
    }

    private static <ReqT, RespT> ServerMethodDefinition<?, ?> wrapMethod(ServerMethodDefinition<ReqT, RespT> method,
            List<ServerInterceptor> interceptors) {
        ServerCallHandler<ReqT, RespT> handler = method.getServerCallHandler();
        for (ServerInterceptor interceptor : interceptors) {
            handler = InternalServerInterceptors.interceptCallHandlerCreate(interceptor, handler);
        }
        ServerMethodDefinition<ReqT, RespT> interceptedDef = method.withServerCallHandler(handler);
        return interceptedDef;
    }
}

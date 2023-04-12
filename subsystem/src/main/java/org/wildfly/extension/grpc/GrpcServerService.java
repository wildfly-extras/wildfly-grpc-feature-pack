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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManager;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.server.Services;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.grpc._private.GrpcLogger;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyServerBuilder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.JdkLoggerFactory;

import static java.util.concurrent.TimeUnit.SECONDS;

public class GrpcServerService implements Service {

    public static final ServiceName SERVICE_NAME = ServiceName.of("grpc-server");

    private static GrpcServerService grpcServerService;
    private static KeyManager keyManager;
    private static TrustManager trustManager;
    private static SSLContext sslContext;
    private static Object monitor = new Object();
    private static boolean restart = true;
    private static boolean serverRestarting;
    private static Set<SSL_ATTRIBUTE> sslUpdates = new HashSet<SSL_ATTRIBUTE>();
    private static Set<SERVER_ATTRIBUTE> serverUpdates = new HashSet<SERVER_ATTRIBUTE>();

    private static int FLOW_CONTROL_WINDOW;
    private static long HANDSHAKE_TIMEOUT;
    private static int INITIAL_FLOW_CONTROL_WINDOW;
    private static long KEEP_ALIVE_TIME;
    private static long KEEP_ALIVE_TIMEOUT;
    private static String KEY_MANAGER_NAME;
    private static int MAX_CONCURRENT_CALLS_PER_CONNECTION;
    private static long MAX_CONNECTION_AGE;
    private static long MAX_CONNECTION_AGE_GRACE;
    private static long MAX_CONNECTION_IDLE;
    private static int MAX_INBOUND_MESSAGE_SIZE;
    private static int MAX_INBOUND_METADATA_SIZE;
    private static long PERMIT_KEEP_ALIVE_TIME;
    private static boolean PERMIT_KEEP_ALIVE_WITHOUT_CALLS;
    private static String PROTOCOL_PROVIDER;
    private static String SERVER_HOST;
    private static int SERVER_PORT;
    private static long SESSION_CACHE_SIZE;
    private static long SESSION_TIMEOUT;
    private static int SHUTDOWN_TIMEOUT;
    private static String SSL_CONTEXT_NAME;
    private static boolean START_TLS;
    private static String TRUST_MANAGER_NAME;

    enum SSL_ATTRIBUTE {
        PROTOCOL_PROVIDER, SESSION_CACHE_SIZE, SESSION_TIMEOUT, START_TLS
    };

    enum SERVER_ATTRIBUTE {
        FLOW_CONTROL_WINDOW, HANDSHAKE_TIMEOUT, INITIAL_FLOW_CONTROL_WINDOW, KEEP_ALIVE_TIME, KEEP_ALIVE_TIMEOUT, KEY_MANAGER_NAME, MAX_CONCURRENT_CALLS_PER_CONNECTION, MAX_CONNECTION_AGE, MAX_CONNECTION_AGE_GRACE, MAX_CONNECTION_IDLE, MAX_INBOUND_MESSAGE_SIZE, MAX_INBOUND_METADATA_SIZE, PERMIT_KEEP_ALIVE_TIME, PERMIT_KEEP_ALIVE_WITHOUT_CALLS, SERVER_HOST, SERVER_PORT, SHUTDOWN_TIMEOUT, TRUST_MANAGER_NAME
    };

    static void configure(ModelNode configuration, OperationContext context) throws OperationFailedException {
        // Initialize the Netty logger factory
        InternalLoggerFactory.setDefaultFactory(JdkLoggerFactory.INSTANCE);

        serverUpdates.clear();
        Integer n = GrpcSubsystemDefinition.GRPC_FLOW_CONTROL_WINDOW.resolveModelAttribute(context, configuration)
                .asIntOrNull();
        if (n != null && n.intValue() != FLOW_CONTROL_WINDOW) {
            FLOW_CONTROL_WINDOW = n;
            serverUpdates.add(SERVER_ATTRIBUTE.FLOW_CONTROL_WINDOW);
            restart = true;
        }
        Long l = GrpcSubsystemDefinition.GRPC_HANDSHAKE_TIMEOUT.resolveModelAttribute(context, configuration).asLongOrNull();
        if (l != null && l.longValue() != HANDSHAKE_TIMEOUT) {
            HANDSHAKE_TIMEOUT = l;
            serverUpdates.add(SERVER_ATTRIBUTE.HANDSHAKE_TIMEOUT);
            restart = true;
        }
        n = GrpcSubsystemDefinition.GRPC_INITIAL_FLOW_CONTROL_WINDOW.resolveModelAttribute(context, configuration)
                .asIntOrNull();
        if (n != null && n.intValue() != INITIAL_FLOW_CONTROL_WINDOW) {
            INITIAL_FLOW_CONTROL_WINDOW = n;
            serverUpdates.add(SERVER_ATTRIBUTE.INITIAL_FLOW_CONTROL_WINDOW);
            restart = true;
        }
        l = GrpcSubsystemDefinition.GRPC_KEEP_ALIVE_TIME.resolveModelAttribute(context, configuration).asLongOrNull();
        if (l != null && l.longValue() != KEEP_ALIVE_TIME) {
            KEEP_ALIVE_TIME = l;
            serverUpdates.add(SERVER_ATTRIBUTE.KEEP_ALIVE_TIME);
            restart = true;
        }
        n = GrpcSubsystemDefinition.GRPC_KEEP_ALIVE_TIMEOUT.resolveModelAttribute(context, configuration).asIntOrNull();
        if (n != null && n.intValue() != KEEP_ALIVE_TIMEOUT) {
            KEEP_ALIVE_TIMEOUT = n;
            serverUpdates.add(SERVER_ATTRIBUTE.KEEP_ALIVE_TIMEOUT);
            restart = true;
        }
        String s = GrpcSubsystemDefinition.GRPC_KEY_MANAGER_NAME.resolveModelAttribute(context, configuration).asStringOrNull();
        if ((s != null && !s.equals(KEY_MANAGER_NAME)) || (KEY_MANAGER_NAME != null && !KEY_MANAGER_NAME.equals(s))) {
            KEY_MANAGER_NAME = s;
            restart = true;
        }
        n = GrpcSubsystemDefinition.GRPC_MAX_CONCURRENT_CALLS_PER_CONNECTION.resolveModelAttribute(context, configuration)
                .asIntOrNull();
        if (n != null && n.intValue() != MAX_CONCURRENT_CALLS_PER_CONNECTION) {
            MAX_CONCURRENT_CALLS_PER_CONNECTION = n;
            serverUpdates.add(SERVER_ATTRIBUTE.MAX_CONCURRENT_CALLS_PER_CONNECTION);
            restart = true;
        }
        l = GrpcSubsystemDefinition.GRPC_MAX_CONNECTION_AGE.resolveModelAttribute(context, configuration).asLongOrNull();
        if (l != null && l.longValue() != MAX_CONNECTION_AGE) {
            MAX_CONNECTION_AGE = l;
            serverUpdates.add(SERVER_ATTRIBUTE.MAX_CONNECTION_AGE);
            restart = true;
        }
        l = GrpcSubsystemDefinition.GRPC_MAX_CONNECTION_AGE_GRACE.resolveModelAttribute(context, configuration).asLongOrNull();
        if (l != null && n.longValue() != MAX_CONNECTION_AGE_GRACE) {
            MAX_CONNECTION_AGE_GRACE = l;
            serverUpdates.add(SERVER_ATTRIBUTE.MAX_CONNECTION_AGE_GRACE);
            restart = true;
        }
        l = GrpcSubsystemDefinition.GRPC_MAX_CONNECTION_IDLE.resolveModelAttribute(context, configuration).asLongOrNull();
        if (l != null && l.longValue() != MAX_CONNECTION_IDLE) {
            MAX_CONNECTION_IDLE = l;
            serverUpdates.add(SERVER_ATTRIBUTE.MAX_CONNECTION_IDLE);
            restart = true;
        }
        n = GrpcSubsystemDefinition.GRPC_MAX_INBOUND_MESSAGE_SIZE.resolveModelAttribute(context, configuration).asIntOrNull();
        if (n != null && n.intValue() != MAX_INBOUND_MESSAGE_SIZE) {
            MAX_INBOUND_MESSAGE_SIZE = n;
            serverUpdates.add(SERVER_ATTRIBUTE.MAX_INBOUND_MESSAGE_SIZE);
            restart = true;
        }
        n = GrpcSubsystemDefinition.GRPC_MAX_INBOUND_METADATA_SIZE.resolveModelAttribute(context, configuration).asIntOrNull();
        if (n != null && n.intValue() != MAX_INBOUND_METADATA_SIZE) {
            MAX_INBOUND_METADATA_SIZE = n;
            serverUpdates.add(SERVER_ATTRIBUTE.MAX_INBOUND_METADATA_SIZE);
            restart = true;
        }
        l = GrpcSubsystemDefinition.GRPC_PERMIT_KEEP_ALIVE_TIME.resolveModelAttribute(context, configuration).asLongOrNull();
        if (l != null && l.longValue() != PERMIT_KEEP_ALIVE_TIME) {
            PERMIT_KEEP_ALIVE_TIME = l;
            serverUpdates.add(SERVER_ATTRIBUTE.PERMIT_KEEP_ALIVE_TIME);
            restart = true;
        }
        Boolean b = GrpcSubsystemDefinition.GRPC_PERMIT_KEEP_ALIVE_WITHOUT_CALLS.resolveModelAttribute(context, configuration)
                .asBooleanOrNull();
        if (b != null && b.booleanValue() != PERMIT_KEEP_ALIVE_WITHOUT_CALLS) {
            PERMIT_KEEP_ALIVE_WITHOUT_CALLS = b;
            serverUpdates.add(SERVER_ATTRIBUTE.PERMIT_KEEP_ALIVE_WITHOUT_CALLS);
            restart = true;
        }
        s = GrpcSubsystemDefinition.GRPC_PROTOCOL_PROVIDER.resolveModelAttribute(context, configuration).asStringOrNull();
        if ((s != null && !s.equals(PROTOCOL_PROVIDER)) || (PROTOCOL_PROVIDER != null && !PROTOCOL_PROVIDER.equals(s))) {
            PROTOCOL_PROVIDER = s;
            sslUpdates.add(SSL_ATTRIBUTE.PROTOCOL_PROVIDER);
            restart = true;
        }
        s = GrpcSubsystemDefinition.GRPC_SERVER_HOST.resolveModelAttribute(context, configuration).asStringOrNull();
        if ((s != null && !s.equals(SERVER_HOST)) || (SERVER_HOST != null && !SERVER_HOST.equals(s))) {
            SERVER_HOST = s;
            restart = true;
        }
        n = GrpcSubsystemDefinition.GRPC_SERVER_PORT.resolveModelAttribute(context, configuration).asIntOrNull();
        if (n != null && n.intValue() != SERVER_PORT) {
            SERVER_PORT = n;
            restart = true;
        }
        l = GrpcSubsystemDefinition.GRPC_SESSION_CACHE_SIZE.resolveModelAttribute(context, configuration).asLongOrNull();
        if (l != null && l.longValue() != SESSION_CACHE_SIZE) {
            SESSION_CACHE_SIZE = l;
            sslUpdates.add(SSL_ATTRIBUTE.SESSION_CACHE_SIZE);
            restart = true;
        }
        l = GrpcSubsystemDefinition.GRPC_SESSION_TIMEOUT.resolveModelAttribute(context, configuration).asLongOrNull();
        if (l != null && l.longValue() != SESSION_TIMEOUT) {
            SESSION_TIMEOUT = l;
            sslUpdates.add(SSL_ATTRIBUTE.SESSION_TIMEOUT);
            restart = true;
        }
        n = GrpcSubsystemDefinition.GRPC_SHUTDOWN_TIMEOUT.resolveModelAttribute(context, configuration).asIntOrNull();
        if (n != null && n.intValue() != SHUTDOWN_TIMEOUT) {
            SHUTDOWN_TIMEOUT = n;
            restart = true;
        }
        s = GrpcSubsystemDefinition.GRPC_SSL_CONTEXT_NAME.resolveModelAttribute(context, configuration).asStringOrNull();
        if ((s != null && !s.equals(SSL_CONTEXT_NAME)) || (SSL_CONTEXT_NAME != null && !SSL_CONTEXT_NAME.equals(s))) {
            SSL_CONTEXT_NAME = s;
            restart = true;
        }
        b = GrpcSubsystemDefinition.GRPC_START_TLS.resolveModelAttribute(context, configuration).asBooleanOrNull();
        if (b != null && b.booleanValue() != START_TLS) {
            START_TLS = b;
            sslUpdates.add(SSL_ATTRIBUTE.START_TLS);
            restart = true;
        }
        s = GrpcSubsystemDefinition.GRPC_TRUST_MANAGER_NAME.resolveModelAttribute(context, configuration).asStringOrNull();
        if ((s != null && !s.equals(TRUST_MANAGER_NAME)) || (TRUST_MANAGER_NAME != null && !TRUST_MANAGER_NAME.equals(s))) {
            TRUST_MANAGER_NAME = s;
            restart = true;
        }
    }

    public static void install(ServiceTarget serviceTarget, DeploymentUnit deploymentUnit, List<Class<?>> serviceClasses)
            throws Exception {
        if (grpcServerService == null || (restart && !serverRestarting)) {
            synchronized (monitor) {
                if (grpcServerService == null || (restart && !serverRestarting)) {
                    serverRestarting = true;
                    restart = false;

                    // setup service
                    ServiceName serviceName = deploymentUnit.getServiceName().append(SERVICE_NAME);
                    ServiceBuilder<?> serviceBuilder = serviceTarget.addService(serviceName);
                    Consumer<GrpcServerService> serviceConsumer = serviceBuilder.provides(serviceName);

                    // wire dependencies
                    Supplier<ExecutorService> executorSupplier = Services.requireServerExecutor(serviceBuilder);
                    CapabilityServiceSupport css = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);
                    if (KEY_MANAGER_NAME != null && !"".equals(KEY_MANAGER_NAME)) {
                        ServiceName keyManagerName = css.getCapabilityServiceName(Capabilities.KEY_MANAGER_CAPABILITY,
                                KEY_MANAGER_NAME);
                        Supplier<KeyManager> keyManagerSupplier = serviceBuilder.requires(keyManagerName);
                        if (keyManagerSupplier != null) {
                            keyManager = keyManagerSupplier.get();
                        }
                    } else {
                        keyManager = null;
                    }
                    if (keyManager != null && SSL_CONTEXT_NAME != null && !"".equals(SSL_CONTEXT_NAME)) {
                        ServiceName sslContextName = css.getCapabilityServiceName(Capabilities.SSL_CONTEXT_CAPABILITY,
                                SSL_CONTEXT_NAME);
                        Supplier<SSLContext> sslContextSupplier = serviceBuilder.requires(sslContextName);
                        if (sslContextSupplier != null) {
                            sslContext = sslContextSupplier.get();
                        }
                    } else {
                        sslContext = null;
                    }
                    if (keyManager != null && TRUST_MANAGER_NAME != null && !"".equals(TRUST_MANAGER_NAME)) {
                        ServiceName trustManagerName = css.getCapabilityServiceName(Capabilities.TRUST_MANAGER_CAPABILITY,
                                TRUST_MANAGER_NAME);
                        Supplier<TrustManager> trustManagerSupplier = serviceBuilder.requires(trustManagerName);
                        if (trustManagerSupplier != null) {
                            trustManager = trustManagerSupplier.get();
                        }
                    } else {
                        trustManager = null;
                    }
                    // stop running service
                    if (grpcServerService != null) {
                        grpcServerService.stopServer();
                    }
                    // install service
                    grpcServerService = new GrpcServerService(deploymentUnit.getName(), serviceConsumer, executorSupplier,
                            serviceClasses);
                    serviceBuilder.setInstance(grpcServerService);
                    serviceBuilder.install();
                    return;
                }
            }
        }
        grpcServerService.addServiceClasses(serviceClasses);
    }

    private final String name;
    private final Consumer<GrpcServerService> serverService;
    private final Supplier<ExecutorService> executorService;
    private final Set<Class<?>> serviceClasses = new HashSet<Class<?>>();
    private final Set<BindableService> services = new HashSet<BindableService>();
    private Server server;

    private GrpcServerService(String name, Consumer<GrpcServerService> serverService,
            Supplier<ExecutorService> executorService,
            List<Class<?>> serviceClasses) throws Exception {
        this.name = name;
        this.serverService = serverService;
        this.executorService = executorService;
        for (Class<?> serviceClass : serviceClasses) {
            newService(serviceClass, this.serviceClasses, services);
        }
    }

    @Override
    public void start(StartContext context) {
        context.asynchronous();
        executorService.get().submit(() -> {
            try {
                startServer();
                context.complete();
                serverRestarting = false;
            } catch (Throwable e) {
                context.failed(new StartException(e));
            }
        });
        serverService.accept(this);
    }

    void addServiceClasses(List<Class<?>> serviceClasses) throws Exception {
        for (Class<?> serviceClass : serviceClasses) {
            newService(serviceClass, this.serviceClasses, services);
        }
    }

    private void startServer() throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        GrpcLogger.LOGGER.serverListening(name, SERVER_HOST, SERVER_PORT);
        SocketAddress socketAddress = new InetSocketAddress(SERVER_HOST, SERVER_PORT);
        NettyServerBuilder serverBuilder = NettyServerBuilder.forAddress(socketAddress);

        for (SERVER_ATTRIBUTE attr : serverUpdates) {
            switch (attr) {
                case FLOW_CONTROL_WINDOW:
                    serverBuilder.flowControlWindow(FLOW_CONTROL_WINDOW);
                    break;
                case HANDSHAKE_TIMEOUT:
                    serverBuilder.handshakeTimeout(HANDSHAKE_TIMEOUT, SECONDS);
                    break;
                case INITIAL_FLOW_CONTROL_WINDOW:
                    serverBuilder.initialFlowControlWindow(INITIAL_FLOW_CONTROL_WINDOW);
                    break;
                case KEEP_ALIVE_TIME:
                    serverBuilder.keepAliveTime(KEEP_ALIVE_TIME, SECONDS);
                    break;
                case KEEP_ALIVE_TIMEOUT:
                    serverBuilder.keepAliveTimeout(KEEP_ALIVE_TIMEOUT, SECONDS);
                    break;
                case KEY_MANAGER_NAME: // Shouldn't be in serverUpdates
                    break;
                case MAX_CONCURRENT_CALLS_PER_CONNECTION:
                    serverBuilder.maxConcurrentCallsPerConnection(MAX_CONCURRENT_CALLS_PER_CONNECTION);
                    break;
                case MAX_CONNECTION_AGE:
                    serverBuilder.maxConnectionAge(MAX_CONNECTION_AGE, SECONDS);
                    break;
                case MAX_CONNECTION_AGE_GRACE:
                    serverBuilder.maxConnectionAgeGrace(MAX_CONNECTION_AGE_GRACE, SECONDS);
                    break;
                case MAX_CONNECTION_IDLE:
                    serverBuilder.maxConnectionIdle(MAX_CONNECTION_IDLE, SECONDS);
                    break;
                case MAX_INBOUND_MESSAGE_SIZE:
                    serverBuilder.maxInboundMessageSize(MAX_INBOUND_MESSAGE_SIZE);
                    break;
                case MAX_INBOUND_METADATA_SIZE:
                    serverBuilder.maxInboundMetadataSize(MAX_INBOUND_METADATA_SIZE);
                    break;
                case PERMIT_KEEP_ALIVE_TIME:
                    serverBuilder.permitKeepAliveTime(PERMIT_KEEP_ALIVE_TIME, SECONDS);
                    break;
                case PERMIT_KEEP_ALIVE_WITHOUT_CALLS:
                    serverBuilder.permitKeepAliveWithoutCalls(PERMIT_KEEP_ALIVE_WITHOUT_CALLS);
                    break;
                case SERVER_HOST: // Shouldn't be in serverUpdates
                    break;
                case SERVER_PORT: // Shouldn't be in serverUpdates
                    break;
                case SHUTDOWN_TIMEOUT: // Shouldn't be in serverUpdates
                    break;
                case TRUST_MANAGER_NAME: // Shouldn't be in serverUpdates
                    break;

                default:
                    GrpcLogger.LOGGER.unknownAttribute(attr.toString());
                    break;
            }
        }

        if (keyManager != null && !"".equals(keyManager)) {
            serverBuilder.sslContext(getSslContext(sslContext));
        }

        for (BindableService serviceClass : services) {
            serverBuilder.addService(serviceClass);
            // GrpcLogger.LOGGER.registerService(serviceClass);
        }
        server = serverBuilder.build().start();
    }

    @SuppressWarnings("deprecation")
    private void newService(Class<?> serviceClass, Set<Class<?>> serviceClasses, Set<BindableService> services)
            throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        if (serviceClasses.contains(serviceClass)) {
            return;
        }
        serviceClasses.add(serviceClass);
        Object instance = serviceClass.newInstance();
        services.add((BindableService) instance);
    }

    @Override
    public void stop(final StopContext context) {
        GrpcLogger.LOGGER.serverStopping(name);
        if (server != null) {
            stopServer();
        }
        serverService.accept(null);
    }

    private void stopServer() {
        try {
            if (server != null) {
                server.shutdown().awaitTermination(SHUTDOWN_TIMEOUT, SECONDS);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private SslContext getSslContext(SSLContext sslContext) throws SSLException {
        SslContextBuilder sslContextBuilder = SslContextBuilder.forServer(keyManager);
        if (sslContext != null) {
            sslContextBuilder.sslContextProvider(sslContext.getProvider());
            SSLEngine sslEngine = sslContext.createSSLEngine();
            sslContextBuilder.ciphers(Arrays.asList(sslEngine.getEnabledCipherSuites()));
            sslContextBuilder.protocols(sslContext.getDefaultSSLParameters().getApplicationProtocols());
            if (trustManager != null) {
                sslContextBuilder.trustManager(trustManager);
            }
        }
        for (SSL_ATTRIBUTE attr : sslUpdates) {
            switch (attr) {
                case PROTOCOL_PROVIDER:
                    sslContextBuilder.sslProvider(SslProvider.valueOf(PROTOCOL_PROVIDER));
                    break;
                case SESSION_CACHE_SIZE:
                    sslContextBuilder.sessionCacheSize(SESSION_CACHE_SIZE);
                    break;
                case SESSION_TIMEOUT:
                    sslContextBuilder.sessionTimeout(SESSION_TIMEOUT);
                    break;
                case START_TLS:
                    sslContextBuilder.startTls(START_TLS);
                    break;
                default:
                    GrpcLogger.LOGGER.unknownAttribute(attr.toString());
                    break;
            }
        }
        sslContextBuilder = GrpcSslContexts.configure(sslContextBuilder);
        return sslContextBuilder.build();
    }
}

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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.net.ssl.KeyManager;

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
import io.netty.handler.ssl.SslContextBuilder;

import static java.util.concurrent.TimeUnit.SECONDS;

public class GrpcServerService implements Service {

    public static final ServiceName SERVICE_NAME = ServiceName.of("grpc-server");

    private static final long SHUTDOWN_TIMEOUT = 3; // seconds
    private static GrpcServerService grpcServerService;
    private static KeyManager keyManager;
    private static Object monitor = new Object();
    private static boolean serverRestarting;
    private static Set<Attribute> updates = new HashSet<Attribute>();

    private static int FLOW_CONTROL_WINDOW;
    private static int HANDSHAKE_TIMEOUT;
    private static int INITIAL_FLOW_CONTROL_WINDOW;
    private static int KEEP_ALIVE_TIME;
    private static int KEEP_ALIVE_TIMEOUT;
    private static String KEY_MANAGER_NAME;
    private static int MAX_CONCURRENT_CALLS_PER_CONNECTION;
    private static int MAX_CONNECTION_AGE;
    private static int MAX_CONNECTION_AGE_GRACE;
    private static int MAX_CONNECTION_IDLE;
    private static int MAX_INBOUND_MESSAGE_SIZE;
    private static int MAX_INBOUND_METADATA_SIZE;
    private static int PERMIT_KEEP_ALIVE_TIME;
    private static boolean PERMIT_KEEP_ALIVE_WITHOUT_CALLS;
    private static String SERVER_HOST;
    private static int SERVER_PORT;

    enum Attribute {
        FLOW_CONTROL_WINDOW, HANDSHAKE_TIMEOUT, INITIAL_FLOW_CONTROL_WINDOW, KEEP_ALIVE_TIME, KEEP_ALIVE_TIMEOUT, KEY_MANAGER_NAME, MAX_CONCURRENT_CALLS_PER_CONNECTION, MAX_CONNECTION_AGE, MAX_CONNECTION_AGE_GRACE, MAX_CONNECTION_IDLE, MAX_INBOUND_MESSAGE_SIZE, MAX_INBOUND_METADATA_SIZE, PERMIT_KEEP_ALIVE_TIME, PERMIT_KEEP_ALIVE_WITHOUT_CALLS, SERVER_HOST, SERVER_PORT
    };

    static void configure(ModelNode configuration, OperationContext context) throws OperationFailedException {
        updates.clear();
        Integer n = GrpcSubsystemDefinition.GRPC_FLOW_CONTROL_WINDOW.resolveModelAttribute(context, configuration)
                .asIntOrNull();
        if (n != null && n.intValue() != FLOW_CONTROL_WINDOW) {
            FLOW_CONTROL_WINDOW = n;
            updates.add(Attribute.FLOW_CONTROL_WINDOW);
        }
        n = GrpcSubsystemDefinition.GRPC_HANDSHAKE_TIMEOUT.resolveModelAttribute(context, configuration).asIntOrNull();
        if (n != null && n.intValue() != HANDSHAKE_TIMEOUT) {
            HANDSHAKE_TIMEOUT = n;
            updates.add(Attribute.HANDSHAKE_TIMEOUT);
        }
        n = GrpcSubsystemDefinition.GRPC_INITIAL_FLOW_CONTROL_WINDOW.resolveModelAttribute(context, configuration)
                .asIntOrNull();
        if (n != null && n.intValue() != INITIAL_FLOW_CONTROL_WINDOW) {
            INITIAL_FLOW_CONTROL_WINDOW = n;
            updates.add(Attribute.INITIAL_FLOW_CONTROL_WINDOW);
        }
        n = GrpcSubsystemDefinition.GRPC_KEEP_ALIVE_TIME.resolveModelAttribute(context, configuration).asIntOrNull();
        if (n != null && n.intValue() != KEEP_ALIVE_TIME) {
            KEEP_ALIVE_TIME = n;
            updates.add(Attribute.KEEP_ALIVE_TIME);
        }
        n = GrpcSubsystemDefinition.GRPC_KEEP_ALIVE_TIMEOUT.resolveModelAttribute(context, configuration).asIntOrNull();
        if (n != null && n.intValue() != KEEP_ALIVE_TIMEOUT) {
            KEEP_ALIVE_TIMEOUT = n;
            updates.add(Attribute.KEEP_ALIVE_TIMEOUT);
        }
        String s = GrpcSubsystemDefinition.GRPC_KEY_MANAGER_NAME.resolveModelAttribute(context, configuration).asStringOrNull();
        if ((s != null && !s.equals(KEY_MANAGER_NAME)) || (KEY_MANAGER_NAME != null && !KEY_MANAGER_NAME.equals(s))) {
            KEY_MANAGER_NAME = s;
            updates.add(Attribute.KEY_MANAGER_NAME);
        }
        n = GrpcSubsystemDefinition.GRPC_MAX_CONCURRENT_CALLS_PER_CONNECTION.resolveModelAttribute(context, configuration)
                .asIntOrNull();
        if (n != null && n.intValue() != MAX_CONCURRENT_CALLS_PER_CONNECTION) {
            MAX_CONCURRENT_CALLS_PER_CONNECTION = n;
            updates.add(Attribute.MAX_CONCURRENT_CALLS_PER_CONNECTION);
        }
        n = GrpcSubsystemDefinition.GRPC_MAX_CONNECTION_AGE.resolveModelAttribute(context, configuration).asIntOrNull();
        if (n != null && n.intValue() != MAX_CONNECTION_AGE) {
            MAX_CONNECTION_AGE = n;
            updates.add(Attribute.MAX_CONNECTION_AGE);
        }
        n = GrpcSubsystemDefinition.GRPC_MAX_CONNECTION_AGE_GRACE.resolveModelAttribute(context, configuration).asIntOrNull();
        if (n != null && n.intValue() != MAX_CONNECTION_AGE_GRACE) {
            MAX_CONNECTION_AGE_GRACE = n;
            updates.add(Attribute.MAX_CONNECTION_AGE_GRACE);
        }
        n = GrpcSubsystemDefinition.GRPC_MAX_CONNECTION_IDLE.resolveModelAttribute(context, configuration).asIntOrNull();
        if (n != null && n.intValue() != MAX_CONNECTION_IDLE) {
            MAX_CONNECTION_IDLE = n;
            updates.add(Attribute.MAX_CONNECTION_IDLE);
        }
        n = GrpcSubsystemDefinition.GRPC_MAX_INBOUND_MESSAGE_SIZE.resolveModelAttribute(context, configuration).asIntOrNull();
        if (n != null && n.intValue() != MAX_INBOUND_MESSAGE_SIZE) {
            MAX_INBOUND_MESSAGE_SIZE = n;
            updates.add(Attribute.MAX_INBOUND_MESSAGE_SIZE);
        }
        n = GrpcSubsystemDefinition.GRPC_MAX_INBOUND_METADATA_SIZE.resolveModelAttribute(context, configuration).asIntOrNull();
        if (n != null && n.intValue() != MAX_INBOUND_METADATA_SIZE) {
            MAX_INBOUND_METADATA_SIZE = n;
            updates.add(Attribute.MAX_INBOUND_METADATA_SIZE);
        }
        n = GrpcSubsystemDefinition.GRPC_PERMIT_KEEP_ALIVE_TIME.resolveModelAttribute(context, configuration).asIntOrNull();
        if (n != null && n.intValue() != PERMIT_KEEP_ALIVE_TIME) {
            PERMIT_KEEP_ALIVE_TIME = n;
            updates.add(Attribute.PERMIT_KEEP_ALIVE_TIME);
        }
        Boolean b = GrpcSubsystemDefinition.GRPC_PERMIT_KEEP_ALIVE_WITHOUT_CALLS.resolveModelAttribute(context, configuration)
                .asBooleanOrNull();
        if (b != null && b.booleanValue() != PERMIT_KEEP_ALIVE_WITHOUT_CALLS) {
            PERMIT_KEEP_ALIVE_WITHOUT_CALLS = b;
            updates.add(Attribute.PERMIT_KEEP_ALIVE_WITHOUT_CALLS);
        }
        s = GrpcSubsystemDefinition.GRPC_SERVER_HOST.resolveModelAttribute(context, configuration).asStringOrNull();
        if ((s != null && !s.equals(SERVER_HOST)) || (SERVER_HOST != null && !SERVER_HOST.equals(s))) {
            SERVER_HOST = s;
            updates.add(Attribute.SERVER_HOST);
        }
        n = GrpcSubsystemDefinition.GRPC_SERVER_PORT.resolveModelAttribute(context, configuration).asIntOrNull();
        if (n != null && n.intValue() != SERVER_PORT) {
            SERVER_PORT = n;
            updates.add(Attribute.SERVER_PORT);
        }
    }

    public static void install(ServiceTarget serviceTarget, DeploymentUnit deploymentUnit, Map<String, String> serviceClasses,
            ClassLoader classLoader) throws Exception {
        if (grpcServerService == null || (!serverRestarting && !updates.isEmpty())) {
            synchronized (monitor) {
                if (grpcServerService == null || (!serverRestarting && !updates.isEmpty())) {
                    serverRestarting = true;
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
                    // stop running service
                    if (grpcServerService != null) {
                        grpcServerService.stopServer();
                    }
                    // install service
                    grpcServerService = new GrpcServerService(deploymentUnit.getName(), serviceConsumer, executorSupplier,
                            serviceClasses, classLoader);
                    serviceBuilder.setInstance(grpcServerService);
                    serviceBuilder.install();
                    return;
                }
            }
        }
        grpcServerService.addServiceClasses(serviceClasses, classLoader);
    }

    private final String name;
    private final Consumer<GrpcServerService> serverService;
    private final Supplier<ExecutorService> executorService;
    private final Set<BindableService> serviceClasses = new HashSet<BindableService>();
    private Server server;

    private GrpcServerService(String name, Consumer<GrpcServerService> serverService, Supplier<ExecutorService> executorService,
            Map<String, String> serviceClasses, ClassLoader classLoader) throws Exception {
        this.name = name;
        this.serverService = serverService;
        this.executorService = executorService;
        for (String serviceClass : serviceClasses.values()) {
            this.serviceClasses.add(newService(serviceClass, classLoader));
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

    void addServiceClasses(Map<String, String> serviceClasses, ClassLoader classLoader) throws Exception {
        for (String serviceClass : serviceClasses.values()) {
            this.serviceClasses.add(newService(serviceClass, classLoader));
        }
    }

    private void startServer() throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        GrpcLogger.LOGGER.serverListening(name, SERVER_HOST, SERVER_PORT);
        SocketAddress socketAddress = new InetSocketAddress(SERVER_HOST, SERVER_PORT);
        NettyServerBuilder serverBuilder = NettyServerBuilder.forAddress(socketAddress);

        for (Attribute attr : updates) {
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
                case KEY_MANAGER_NAME:
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
                case SERVER_HOST:
                    break;
                case SERVER_PORT:
                    break;
                default:
                    GrpcLogger.LOGGER.unknownAttribute(attr.toString());
            }
        }
        updates.clear();

        if (keyManager != null && !"".equals(keyManager)) {
            SslContextBuilder contextBuilder = SslContextBuilder.forServer(keyManager);
            contextBuilder = GrpcSslContexts.configure(contextBuilder);
            serverBuilder.sslContext(contextBuilder.build());
        }

        for (BindableService serviceClass : serviceClasses) {
            serverBuilder.addService(serviceClass);
            // GrpcLogger.LOGGER.registerService(serviceClass);
        }
        server = serverBuilder.build().start();
    }

    @SuppressWarnings("deprecation")
    private BindableService newService(String serviceClass, ClassLoader classLoader)
            throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        Class<?> clazz = classLoader.loadClass(serviceClass);
        Object instance = clazz.newInstance();
        if (!(instance instanceof BindableService)) {
            throw new ClassCastException("gRPC service " + serviceClass + " is not a BindableService!");
        }
        return ((BindableService) instance);
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
}

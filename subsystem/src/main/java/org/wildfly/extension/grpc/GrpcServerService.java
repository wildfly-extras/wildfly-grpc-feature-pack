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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.net.ssl.KeyManager;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.server.Services;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
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
    private static final String HOST = "localhost"; // TODO make configurable
    private static final int PORT = 9555; // TODO make configurable
    private static final String KEY_MANAGER = "grpcKM"; // TODO make configurable
    private static GrpcServerService grpcServerService;
    private static Object monitor = new Object();

    public static void install(ServiceTarget serviceTarget, DeploymentUnit deploymentUnit, Map<String, String> serviceClasses,
            ClassLoader classLoader) throws Exception {
        if (grpcServerService == null) {
            synchronized (monitor) {
                if (grpcServerService == null) {
                    // setup service
                    ServiceName serviceName = deploymentUnit.getServiceName().append(SERVICE_NAME);
                    ServiceBuilder<?> serviceBuilder = serviceTarget.addService(serviceName);
                    Consumer<GrpcServerService> serviceConsumer = serviceBuilder.provides(serviceName);

                    // wire dependencies
                    Supplier<ExecutorService> executorSupplier = Services.requireServerExecutor(serviceBuilder);
                    CapabilityServiceSupport css = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);
                    ServiceName keyManagerName = css.getCapabilityServiceName(Capabilities.KEY_MANAGER_CAPABILITY, KEY_MANAGER);
                    Supplier<KeyManager> keyManagerSupplier = serviceBuilder.requires(keyManagerName);

                    // install service
                    grpcServerService = new GrpcServerService(deploymentUnit.getName(), serviceConsumer, executorSupplier,
                            keyManagerSupplier, serviceClasses, classLoader);
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
    private final Supplier<KeyManager> keyManager;
    private final Set<BindableService> serviceClasses = new HashSet<BindableService>();
    private Server server;

    private GrpcServerService(String name, Consumer<GrpcServerService> serverService, Supplier<ExecutorService> executorService,
            Supplier<KeyManager> keyManager, Map<String, String> serviceClasses, ClassLoader classLoader) throws Exception {
        this.name = name;
        this.serverService = serverService;
        this.executorService = executorService;
        this.keyManager = keyManager;
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
        GrpcLogger.LOGGER.serverListening(name, HOST, PORT);
        NettyServerBuilder serverBuilder = NettyServerBuilder.forPort(PORT);
        SslContextBuilder contextBuilder = SslContextBuilder.forServer(keyManager.get());
        contextBuilder = GrpcSslContexts.configure(contextBuilder);
        serverBuilder.sslContext(contextBuilder.build());

        for (BindableService serviceClass : serviceClasses) {
            serverBuilder.addService(serviceClass);
            GrpcLogger.LOGGER.registerService(serviceClass);
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
            server.shutdown().awaitTermination(SHUTDOWN_TIMEOUT, SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

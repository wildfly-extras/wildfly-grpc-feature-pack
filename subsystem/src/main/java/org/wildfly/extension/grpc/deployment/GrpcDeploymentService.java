/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.grpc.deployment;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.context.spi.CreationalContext;

import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.grpc.WildFlyGrpcDeploymentRegistry;
import org.wildfly.extension.grpc.InterceptorQueue;
import org.wildfly.extension.grpc._private.GrpcLogger;

import io.grpc.BindableService;
import io.grpc.ServerInterceptor;
import io.grpc.ServerServiceDefinition;

class GrpcDeploymentService implements Service {

    private final String deploymentName;
    private final List<Class<? extends BindableService>> serviceClasses;
    private final List<String> interceptorClassNames;
    private final ClassLoader classLoader;
    private final Supplier<WildFlyGrpcDeploymentRegistry> serverServiceSupplier;
    private final Supplier<BeanManager> beanManagerSupplier;

    private final List<ServerServiceDefinition> registeredServices = new ArrayList<>();
    private final List<CreationalContext<?>> creationalContexts = new ArrayList<>();

    GrpcDeploymentService(final String deploymentName,
            final List<Class<? extends BindableService>> serviceClasses,
            final List<String> interceptorClassNames,
            final ClassLoader classLoader,
            final Supplier<WildFlyGrpcDeploymentRegistry> serverServiceSupplier,
            final Supplier<BeanManager> beanManagerSupplier) {
        this.deploymentName = deploymentName;
        this.serviceClasses = serviceClasses;
        this.interceptorClassNames = interceptorClassNames;
        this.classLoader = classLoader;
        this.serverServiceSupplier = serverServiceSupplier;
        this.beanManagerSupplier = beanManagerSupplier;
    }

    @Override
    public void start(final StartContext context) throws StartException {
        final WildFlyGrpcDeploymentRegistry serverService = serverServiceSupplier.get();
        final List<ServerInterceptor> interceptors = createInterceptors();

        for (Class<? extends BindableService> serviceType : serviceClasses) {
            final BindableService instance = createServiceInstance(serviceType);
            GrpcLogger.LOGGER.registerService(serviceType.getName(), deploymentName);
            ServerServiceDefinition ssd = serverService.addService(instance, interceptors);
            registeredServices.add(ssd);
        }
    }

    @Override
    public void stop(final StopContext context) {
        final WildFlyGrpcDeploymentRegistry serverService = serverServiceSupplier.get();
        for (ServerServiceDefinition ssd : registeredServices) {
            serverService.removeService(ssd);
        }
        registeredServices.clear();

        for (CreationalContext<?> ctx : creationalContexts) {
            ctx.release();
        }
        creationalContexts.clear();
    }

    private BindableService createServiceInstance(Class<? extends BindableService> serviceType) throws StartException {
        if (beanManagerSupplier != null) {
            final BeanManager bm = beanManagerSupplier.get();
            if (bm != null) {
                Set<Bean<?>> beans = bm.getBeans(serviceType);
                if (!beans.isEmpty()) {
                    Bean<?> bean = bm.resolve(beans);
                    CreationalContext<?> ctx = bm.createCreationalContext(bean);
                    BindableService instance = (BindableService) bm.getReference(bean, serviceType, ctx);
                    creationalContexts.add(ctx);
                    GrpcLogger.LOGGER.registerCdiService(serviceType.getName(), deploymentName);
                    return instance;
                }
            }
        }
        return createViaReflection(serviceType);
    }

    private BindableService createViaReflection(Class<? extends BindableService> serviceType) throws StartException {
        if (System.getSecurityManager() == null) {
            try {
                final Constructor<? extends BindableService> constructor = serviceType.getConstructor();
                return constructor.newInstance();
            } catch (NoSuchMethodException | InvocationTargetException | InstantiationException
                    | IllegalAccessException e) {
                throw new StartException(GrpcLogger.LOGGER.failedToRegister(e, serviceType.getName(), deploymentName));
            }
        } else {
            return AccessController.doPrivileged((PrivilegedAction<BindableService>) () -> {
                try {
                    final Constructor<? extends BindableService> constructor = serviceType.getConstructor();
                    return constructor.newInstance();
                } catch (NoSuchMethodException | InvocationTargetException | InstantiationException
                        | IllegalAccessException e) {
                    throw GrpcLogger.LOGGER.failedToRegister(e, serviceType.getName(), deploymentName);
                }
            });
        }
    }

    private List<ServerInterceptor> createInterceptors() throws StartException {
        List<Class<? extends ServerInterceptor>> sortedClasses = getInterceptorClasses();
        List<ServerInterceptor> interceptors = new ArrayList<>();
        for (Class<? extends ServerInterceptor> interceptorType : sortedClasses) {
            GrpcLogger.LOGGER.registerServerInterceptor(interceptorType.getName());
            if (System.getSecurityManager() == null) {
                try {
                    final Constructor<? extends ServerInterceptor> constructor = interceptorType.getConstructor();
                    interceptors.add(constructor.newInstance());
                } catch (NoSuchMethodException | InvocationTargetException | InstantiationException
                        | IllegalAccessException e) {
                    throw new StartException(GrpcLogger.LOGGER.failedToRegister(e, interceptorType.getName()));
                }
            } else {
                interceptors.add(AccessController.doPrivileged((PrivilegedAction<ServerInterceptor>) () -> {
                    try {
                        final Constructor<? extends ServerInterceptor> constructor = interceptorType.getConstructor();
                        return constructor.newInstance();
                    } catch (NoSuchMethodException | InvocationTargetException | InstantiationException
                            | IllegalAccessException e) {
                        throw GrpcLogger.LOGGER.failedToRegister(e, interceptorType.getName());
                    }
                }));
            }
        }
        return interceptors;
    }

    private List<Class<? extends ServerInterceptor>> getInterceptorClasses() {
        InterceptorQueue queue = new InterceptorQueue();
        try {
            for (String className : interceptorClassNames) {
                Class<? extends ServerInterceptor> clazz = classLoader.loadClass(className)
                        .asSubclass(ServerInterceptor.class);
                queue.add(clazz);
            }
            return queue.toList();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}

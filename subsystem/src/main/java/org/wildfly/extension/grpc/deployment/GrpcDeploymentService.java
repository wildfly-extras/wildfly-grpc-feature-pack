/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.grpc.deployment;

import static org.wildfly.extension.grpc._private.GrpcLogger.LOGGER;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.AmbiguousResolutionException;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;

import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.grpc.WildFlyGrpcDeploymentRegistry;

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
        try {
            for (Class<? extends BindableService> serviceType : serviceClasses) {
                final List<ServerInterceptor> interceptors = createInterceptors();
                final BindableService instance = createServiceInstance(serviceType);
                LOGGER.debugf("Registering gRPC service %s for deployment %s.", serviceType.getName(), deploymentName);
                ServerServiceDefinition ssd = serverService.addService(deploymentName, instance, interceptors);
                registeredServices.add(ssd);
            }
        } catch (StartException e) {
            for (ServerServiceDefinition ssd : registeredServices) {
                serverService.removeService(ssd);
            }
            registeredServices.clear();
            for (CreationalContext<?> ctx : creationalContexts) {
                ctx.release();
            }
            creationalContexts.clear();
            throw e;
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
                    final Bean<?> bean;
                    try {
                        bean = bm.resolve(beans);
                    } catch (AmbiguousResolutionException e) {
                        throw LOGGER.failedToRegister(e, serviceType.getName(), deploymentName);
                    }
                    CreationalContext<?> ctx = bm.createCreationalContext(bean);
                    BindableService instance = (BindableService) bm.getReference(bean, serviceType, ctx);
                    creationalContexts.add(ctx);
                    LOGGER.debugf("Instantiating gRPC service %s as CDI bean for deployment %s.", serviceType.getName(),
                            deploymentName);
                    return instance;
                }
            }
        }
        return createViaReflection(serviceType);
    }

    private BindableService createViaReflection(Class<? extends BindableService> serviceType) throws StartException {
        try {
            final Constructor<? extends BindableService> constructor = serviceType.getConstructor();
            return constructor.newInstance();
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException
                | IllegalAccessException e) {
            throw LOGGER.failedToRegister(e, serviceType.getName(), deploymentName);
        }
    }

    private List<ServerInterceptor> createInterceptors() throws StartException {
        List<Class<? extends ServerInterceptor>> sortedClasses = getInterceptorClasses();
        List<ServerInterceptor> interceptors = new ArrayList<>();
        for (Class<? extends ServerInterceptor> interceptorType : sortedClasses) {
            LOGGER.debugf("Registering global gRPC ServerInterceptor %s.", interceptorType.getName());
            interceptors.add(createInterceptorInstance(interceptorType));
        }
        return interceptors;
    }

    private ServerInterceptor createInterceptorInstance(Class<? extends ServerInterceptor> interceptorType)
            throws StartException {
        if (beanManagerSupplier != null) {
            final BeanManager bm = beanManagerSupplier.get();
            if (bm != null) {
                Set<Bean<?>> beans = bm.getBeans(interceptorType);
                if (!beans.isEmpty()) {
                    final Bean<?> bean;
                    try {
                        bean = bm.resolve(beans);
                    } catch (AmbiguousResolutionException e) {
                        throw LOGGER.failedToRegister(e, interceptorType.getName(), deploymentName);
                    }
                    CreationalContext<?> ctx = bm.createCreationalContext(bean);
                    ServerInterceptor instance = (ServerInterceptor) bm.getReference(bean, interceptorType, ctx);
                    creationalContexts.add(ctx);
                    LOGGER.debugf("Instantiating gRPC ServerInterceptor %s as CDI bean for deployment %s.",
                            interceptorType.getName(), deploymentName);
                    return instance;
                }
            }
        }
        return createInterceptorViaReflection(interceptorType);
    }

    private ServerInterceptor createInterceptorViaReflection(Class<? extends ServerInterceptor> interceptorType)
            throws StartException {
        try {
            final Constructor<? extends ServerInterceptor> constructor = interceptorType.getConstructor();
            return constructor.newInstance();
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException
                | IllegalAccessException e) {
            throw LOGGER.failedToRegister(e, interceptorType.getName(), deploymentName);
        }
    }

    private List<Class<? extends ServerInterceptor>> getInterceptorClasses() throws StartException {
        List<Class<? extends ServerInterceptor>> classes = new ArrayList<>();
        try {
            for (String className : interceptorClassNames) {
                classes.add(classLoader.loadClass(className).asSubclass(ServerInterceptor.class));
            }
        } catch (ClassNotFoundException e) {
            throw new StartException(e);
        }
        // Sort interceptors by their @Priority annotations.
        // Interceptors with the highest priority comes first.
        // Interceptors with no @Priority comes last.
        classes.sort(Comparator.comparingInt((Class<? extends ServerInterceptor> clazz) -> {
            Priority p = clazz.getAnnotation(Priority.class);
            return p != null ? p.value() : Integer.MAX_VALUE - 1;
        }).reversed());
        return classes;
    }
}

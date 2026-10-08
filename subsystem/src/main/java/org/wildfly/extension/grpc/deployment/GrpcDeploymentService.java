/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.grpc.deployment;

import static org.wildfly.extension.grpc._private.GrpcLogger.LOGGER;

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
            // Interceptors are shared across all services of the deployment — instantiate once, not per service.
            final List<ServerInterceptor> interceptors = createInterceptors();
            for (Class<? extends BindableService> serviceType : serviceClasses) {
                final BindableService instance = createInstance(serviceType);
                LOGGER.debugf("Registering gRPC service %s for deployment %s.", serviceType.getName(), deploymentName);
                ServerServiceDefinition ssd = serverService.addService(deploymentName, instance, interceptors);
                registeredServices.add(ssd);
            }
        } catch (StartException | RuntimeException e) {
            unregister(serverService, e);
            throw e;
        }
    }

    @Override
    public void stop(final StopContext context) {
        unregister(serverServiceSupplier.get(), null);
    }

    private void unregister(final WildFlyGrpcDeploymentRegistry serverService, final Exception cause) {
        if (serverService == null) {
            return;
        }
        registeredServices.forEach(ssd -> runSafely(() -> serverService.removeService(ssd), cause));
        registeredServices.clear();
        creationalContexts.forEach(ctx -> runSafely(ctx::release, cause));
        creationalContexts.clear();
    }

    private static void runSafely(final Runnable action, final Exception cause) {
        try {
            action.run();
        } catch (RuntimeException suppressed) {
            if (cause != null) {
                cause.addSuppressed(suppressed);
            } else {
                LOGGER.failedToCleanupGrpcResources(suppressed);
            }
        }
    }

    private List<ServerInterceptor> createInterceptors() throws StartException {
        List<Class<? extends ServerInterceptor>> classes = new ArrayList<>();
        try {
            for (String className : interceptorClassNames) {
                classes.add(classLoader.loadClass(className).asSubclass(ServerInterceptor.class));
            }
        } catch (ClassNotFoundException e) {
            throw new StartException(e);
        }
        // Sort by @Priority descending; unannotated interceptors come last.
        classes.sort(Comparator.comparingInt((Class<? extends ServerInterceptor> clazz) -> {
            Priority p = clazz.getAnnotation(Priority.class);
            return p != null ? p.value() : Integer.MAX_VALUE;
        }).reversed());
        List<ServerInterceptor> interceptors = new ArrayList<>();
        for (Class<? extends ServerInterceptor> interceptorType : classes) {
            LOGGER.debugf("Registering global gRPC ServerInterceptor %s.", interceptorType.getName());
            interceptors.add(createInstance(interceptorType));
        }
        return interceptors;
    }

    private <T> T createInstance(Class<T> type) throws StartException {
        if (beanManagerSupplier != null) {
            final BeanManager bm = beanManagerSupplier.get();
            if (bm == null) {
                LOGGER.debugf("BeanManager not available for deployment %s; instantiating %s via reflection.",
                        deploymentName, type.getName());
            } else {
                Set<Bean<?>> beans = bm.getBeans(type);
                if (!beans.isEmpty()) {
                    final Bean<?> bean;
                    try {
                        bean = bm.resolve(beans);
                    } catch (AmbiguousResolutionException e) {
                        throw LOGGER.failedToRegister(e, type.getName(), deploymentName);
                    }
                    CreationalContext<?> ctx = bm.createCreationalContext(bean);
                    T instance = type.cast(bm.getReference(bean, type, ctx));
                    creationalContexts.add(ctx);
                    LOGGER.debugf("Instantiating %s as CDI bean for deployment %s.", type.getName(), deploymentName);
                    return instance;
                }
            }
        }
        try {
            return type.getConstructor().newInstance();
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException
                | IllegalAccessException e) {
            throw LOGGER.failedToRegister(e, type.getName(), deploymentName);
        }
    }

}

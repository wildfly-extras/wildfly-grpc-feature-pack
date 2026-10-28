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
import org.wildfly.extension.grpc.ElytronSecurityIdentityInterceptor;
import org.wildfly.extension.grpc.WildFlyGrpcDeploymentRegistry;

import io.grpc.BindableService;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;
import io.grpc.util.MutableHandlerRegistry;

class GrpcDeploymentService implements Service {

    private final String deploymentName;
    private final List<Class<? extends BindableService>> serviceClasses;
    private final List<String> interceptorClassNames;
    private final ClassLoader classLoader;
    private final Supplier<WildFlyGrpcDeploymentRegistry> routingRegistrySupplier;
    private final Supplier<BeanManager> beanManagerSupplier;
    private final MutableHandlerRegistry handlerRegistry;
    private final String contextPath;

    private final List<ServerServiceDefinition> registeredServices = new ArrayList<>();
    private final List<CreationalContext<?>> creationalContexts = new ArrayList<>();

    GrpcDeploymentService(final String deploymentName,
            final List<Class<? extends BindableService>> serviceClasses,
            final List<String> interceptorClassNames,
            final ClassLoader classLoader,
            final Supplier<WildFlyGrpcDeploymentRegistry> routingRegistrySupplier,
            final Supplier<BeanManager> beanManagerSupplier,
            final MutableHandlerRegistry handlerRegistry,
            final String contextPath) {
        this.deploymentName = deploymentName;
        this.serviceClasses = serviceClasses;
        this.interceptorClassNames = interceptorClassNames;
        this.classLoader = classLoader;
        this.routingRegistrySupplier = routingRegistrySupplier;
        this.beanManagerSupplier = beanManagerSupplier;
        this.handlerRegistry = handlerRegistry;
        this.contextPath = contextPath;
    }

    @Override
    public void start(final StartContext context) throws StartException {
        final WildFlyGrpcDeploymentRegistry routingRegistry = routingRegistrySupplier.get();
        if (!routingRegistry.isHttp2Configured()) {
            LOGGER.noHttp2ListenerConfigured(deploymentName);
        }

        try {
            final List<ServerInterceptor> interceptors = createInterceptors();
            // Prepend the identity bridge interceptor so GrpcSecurity.SECURITY_IDENTITY_KEY is
            // populated from the Elytron thread-local set by ElytronContextAssociationHandler.
            // This runs in interceptCall() on the Undertow request thread, before gRPC dispatches
            // the call to its executor — so the security context is captured before thread switch.
            final List<ServerInterceptor> allInterceptors = new ArrayList<>();
            allInterceptors.add(ElytronSecurityIdentityInterceptor.INSTANCE);
            allInterceptors.addAll(interceptors);
            for (Class<? extends BindableService> serviceType : serviceClasses) {
                final BindableService instance = createInstance(serviceType);
                final ServerServiceDefinition ssd = ServerInterceptors.intercept(instance, allInterceptors);
                // Register with the routing registry first. If this throws (duplicate service name
                // across deployments), ssd is not yet in registeredServices, so the catch-block
                // cleanup will not call unregisterService() for this entry — preserving the
                // routing entry that was legitimately registered by another deployment.
                routingRegistry.registerService(ssd.getServiceDescriptor().getName(), contextPath);
                handlerRegistry.addService(ssd);
                registeredServices.add(ssd);
                LOGGER.grpcServiceRegistered(ssd.getServiceDescriptor().getName(), deploymentName, contextPath);
            }
        } catch (StartException | RuntimeException e) {
            unregister(routingRegistry, e);
            throw e;
        }
    }

    @Override
    public void stop(final StopContext context) {
        unregister(routingRegistrySupplier.get(), null);
    }

    private void unregister(final WildFlyGrpcDeploymentRegistry routingRegistry, final Exception cause) {
        if (routingRegistry != null) {
            registeredServices.forEach(ssd -> runSafely(
                    () -> routingRegistry.unregisterService(ssd.getServiceDescriptor().getName()), cause));
        }
        registeredServices.forEach(ssd -> runSafely(() -> handlerRegistry.removeService(ssd), cause));
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

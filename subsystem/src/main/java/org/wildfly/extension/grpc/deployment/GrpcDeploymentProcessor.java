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
import java.util.stream.Collectors;

import org.jboss.as.controller.PathElement;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentResourceSupport;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.dmr.ModelNode;
import org.jboss.jandex.DotName;
import org.jboss.modules.Module;
import org.wildfly.extension.grpc.Constants;
import org.wildfly.extension.grpc.GrpcExtension;
import org.wildfly.extension.grpc.InterceptorQueue;
import org.wildfly.extension.grpc.WildFlyGrpcDeploymentRegistry;
import org.wildfly.extension.grpc._private.GrpcLogger;

import io.grpc.BindableService;
import io.grpc.ServerInterceptor;

public class GrpcDeploymentProcessor implements DeploymentUnitProcessor {

    private static final DotName BINDABLE_CLASS = DotName.createSimple(BindableService.class.getName());
    private static final DotName SERVER_INTERCEPTOR_CLASS = DotName.createSimple(ServerInterceptor.class.getName());

    private final WildFlyGrpcDeploymentRegistry registry;

    public GrpcDeploymentProcessor(final WildFlyGrpcDeploymentRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final CompositeIndex index = deploymentUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
        List<String> serviceClasses = index.getAllKnownImplementors(BINDABLE_CLASS).stream()
                .map(ci -> ci.name().toString()).collect(Collectors.toList());
        Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        List<Class<? extends BindableService>> leaves = getLeaves(serviceClasses, module.getClassLoader());
        processManagement(deploymentUnit, leaves);

        List<String> interceptorClasses = index.getAllKnownImplementors(SERVER_INTERCEPTOR_CLASS).stream()
                .map(ci -> ci.name().toString()).collect(Collectors.toList());

        for (Class<? extends BindableService> type : getLeaves(serviceClasses, module.getClassLoader())) {
            registry.addService(deploymentUnit, type, getInterceptors(interceptorClasses, module.getClassLoader()));
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
        registry.removeDeploymentServices(context);
    }

    private List<Class<? extends BindableService>> getLeaves(List<String> classNames, ClassLoader classLoader) {
        List<Class<? extends BindableService>> classes = new ArrayList<>();
        try {
            for (String s : classNames) {
                classes.add(classLoader.loadClass(s).asSubclass(BindableService.class));
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        List<Class<? extends BindableService>> leaves = new ArrayList<>();
        for (Class<? extends BindableService> clazz : classes) {
            if (isLeaf(clazz, classes)) {
                leaves.add(clazz);
            }
        }
        return leaves;
    }

    private boolean isLeaf(Class<?> clazz, List<Class<? extends BindableService>> classes) {
        for (Class<?> c : classes) {
            if (clazz != c && clazz.isAssignableFrom(c)) {
                return false;
            }
        }
        return true;
    }

    private List<Class<? extends ServerInterceptor>> getInterceptorClasses(List<String> classNames,
            ClassLoader classLoader) {
        InterceptorQueue classes = new InterceptorQueue();
        try {
            for (String s : classNames) {
                Class<? extends ServerInterceptor> clazz = classLoader.loadClass(s).asSubclass(ServerInterceptor.class);
                classes.add(clazz);
            }
            return classes.toList();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private List<ServerInterceptor> getInterceptors(List<String> classNames, ClassLoader classLoader) {
        List<Class<? extends ServerInterceptor>> classes = getInterceptorClasses(classNames, classLoader);
        List<ServerInterceptor> interceptors = new ArrayList<ServerInterceptor>();
        for (int i = 0; i < classes.size(); i++) {
            Class<? extends ServerInterceptor> interceptorType = classes.get(i);
            GrpcLogger.LOGGER.registerServerInterceptor(interceptorType.getName());
            final ServerInterceptor serverInterceptor;
            if (System.getSecurityManager() == null) {
                try {
                    final Constructor<? extends ServerInterceptor> constructor = interceptorType.getConstructor();
                    serverInterceptor = constructor.newInstance();
                } catch (NoSuchMethodException | InvocationTargetException | InstantiationException
                        | IllegalAccessException e) {
                    throw GrpcLogger.LOGGER.failedToRegister(e, interceptorType.getName());
                }
            } else {
                serverInterceptor = AccessController.doPrivileged((PrivilegedAction<ServerInterceptor>) () -> {
                    try {
                        final Constructor<? extends ServerInterceptor> constructor = interceptorType.getConstructor();
                        return constructor.newInstance();
                    } catch (NoSuchMethodException | InvocationTargetException | InstantiationException
                            | IllegalAccessException e) {
                        throw GrpcLogger.LOGGER.failedToRegister(e, interceptorType.getName());
                    }
                });
            }
            interceptors.add(serverInterceptor);
        }
        return interceptors;
    }

    private void processManagement(DeploymentUnit deploymentUnit,
            List<Class<? extends BindableService>> grpcServiceClasses) {
        DeploymentResourceSupport drs = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_RESOURCE_SUPPORT);

        for (Class<?> clazz : grpcServiceClasses) {
            ModelNode serviceModel = drs.getDeploymentSubModel(GrpcExtension.SUBSYSTEM_NAME,
                    PathElement.pathElement(Constants.GRPC_SERVICE, clazz.getSimpleName()));
            serviceModel.get(Constants.SERVICE_CLASS).set(clazz.getName());
        }
    }
}

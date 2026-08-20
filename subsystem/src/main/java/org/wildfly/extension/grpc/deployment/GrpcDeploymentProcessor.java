/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.grpc.deployment;

import java.lang.reflect.Modifier;
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

import io.grpc.BindableService;
import io.grpc.ServerInterceptor;

public class GrpcDeploymentProcessor implements DeploymentUnitProcessor {

    private static final DotName BINDABLE_CLASS = DotName.createSimple(BindableService.class.getName());
    private static final DotName SERVER_INTERCEPTOR_CLASS = DotName.createSimple(ServerInterceptor.class.getName());

    public GrpcDeploymentProcessor() {
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final CompositeIndex index = deploymentUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
        List<String> serviceClasses = index.getAllKnownImplementors(BINDABLE_CLASS).stream()
                .map(ci -> ci.name().toString()).collect(Collectors.toList());
        Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        List<Class<? extends BindableService>> leaves = getLeaves(serviceClasses, module.getClassLoader());

        if (leaves.isEmpty()) {
            return;
        }

        processManagement(deploymentUnit, leaves);

        List<String> interceptorClasses = index.getAllKnownImplementors(SERVER_INTERCEPTOR_CLASS).stream()
                .map(ci -> ci.name().toString()).collect(Collectors.toList());

        deploymentUnit.putAttachment(GrpcDeploymentAttachments.GRPC_BINDABLE_SERVICES, leaves);
        deploymentUnit.putAttachment(GrpcDeploymentAttachments.INTERCEPTOR_CLASSES, interceptorClasses);
    }

    @Override
    public void undeploy(DeploymentUnit context) {
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
            if (!Modifier.isAbstract(clazz.getModifiers()) && isLeaf(clazz, classes)) {
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

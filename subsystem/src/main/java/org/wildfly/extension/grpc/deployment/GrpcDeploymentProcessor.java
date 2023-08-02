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
package org.wildfly.extension.grpc.deployment;

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
import org.wildfly.extension.grpc.WildFlyGrpcDeploymentRegistry;

import io.grpc.BindableService;

public class GrpcDeploymentProcessor implements DeploymentUnitProcessor {

    private static final DotName BINDABLE_CLASS = DotName.createSimple(BindableService.class.getName());

    private final WildFlyGrpcDeploymentRegistry registry;

    public GrpcDeploymentProcessor(final WildFlyGrpcDeploymentRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final CompositeIndex index = deploymentUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
        List<String> serviceClasses = index.getAllKnownImplementors(BINDABLE_CLASS)
                .stream()
                .map(ci -> ci.name().toString())
                .collect(Collectors.toList());
        Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        List<Class<? extends BindableService>> leaves = getLeaves(serviceClasses, module.getClassLoader());
        processManagement(deploymentUnit, leaves);
        for (Class<? extends BindableService> type : getLeaves(serviceClasses, module.getClassLoader())) {
            registry.addService(deploymentUnit, type);
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

    private void processManagement(DeploymentUnit deploymentUnit, List<Class<? extends BindableService>> grpcServiceClasses) {
        DeploymentResourceSupport drs = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_RESOURCE_SUPPORT);

        for (Class<?> clazz : grpcServiceClasses) {
            ModelNode serviceModel = drs.getDeploymentSubModel(GrpcExtension.SUBSYSTEM_NAME,
                    PathElement.pathElement(Constants.GRPC_SERVICE, clazz.getSimpleName()));
            serviceModel.get(Constants.SERVICE_CLASS).set(clazz.getName());
        }
    }
}

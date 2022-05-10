/*
 *  Copyright 2022 Red Hat, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.wildfly.extension.grpc.deployment;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jboss.as.controller.PathElement;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentResourceSupport;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.dmr.ModelNode;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.extension.grpc.Constants;
import org.wildfly.extension.grpc.GrpcExtension;
import org.wildfly.extension.grpc.GrpcServerService;

public class GrpcDeploymentProcessor implements DeploymentUnitProcessor {

    static final DotName GRPC_SERVICE = DotName.createSimple("org.wildfly.grpc.GrpcService");

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        CompositeIndex compositeIndex = deploymentUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
        if (compositeIndex.getAnnotations(GRPC_SERVICE).isEmpty()) {
            return;
        }

        List<AnnotationInstance> serviceAnnotations = compositeIndex.getAnnotations(GRPC_SERVICE);
        if (serviceAnnotations == null || serviceAnnotations.isEmpty()) {
            return;
        }

        Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        Map<String, String> serviceClasses = serviceAnnotations.stream()
                .filter(annotationInstance -> annotationInstance.target() instanceof ClassInfo)
                .map(annotationInstance -> (ClassInfo) annotationInstance.target())
                .collect(Collectors.toMap(ClassInfo::simpleName, clazz -> clazz.name().toString()));
        processManagement(deploymentUnit, serviceClasses);

        // Config config = ConfigProvider.getConfig(module.getClassLoader());
        // System.out.println("wildfly.grpc.server.host: " +
        // config.getConfigValue("wildfly.grpc.server.host").getValue());

        ServiceTarget serviceTarget = phaseContext.getServiceTarget();
        GrpcServerService.install(serviceTarget, deploymentUnit, serviceClasses, module.getClassLoader());
    }

    private void processManagement(DeploymentUnit deploymentUnit, Map<String, String> grpcServiceClasses) {
        DeploymentResourceSupport drs = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_RESOURCE_SUPPORT);

        for (Map.Entry<String, String> entry : grpcServiceClasses.entrySet()) {
            ModelNode serviceModel = drs.getDeploymentSubModel(GrpcExtension.SUBSYSTEM_NAME,
                    PathElement.pathElement(Constants.GRPC_SERVICE, entry.getKey()));
            serviceModel.get(Constants.SERVICE_CLASS).set(entry.getValue());
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }
}

/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.grpc.deployment;

import static org.wildfly.extension.grpc._private.GrpcLogger.LOGGER;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import jakarta.enterprise.inject.spi.BeanManager;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.weld.WeldCapability;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.grpc.Capabilities;
import org.wildfly.extension.grpc.GrpcSubsystemDefinition;
import org.wildfly.extension.grpc.WildFlyGrpcDeploymentRegistry;

import io.grpc.BindableService;

public class GrpcServiceInstallProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final List<Class<? extends BindableService>> services = deploymentUnit
                .getAttachment(GrpcDeploymentAttachments.GRPC_BINDABLE_SERVICES);

        if (services == null || services.isEmpty()) {
            return;
        }

        final List<String> interceptorClasses = deploymentUnit
                .getAttachment(GrpcDeploymentAttachments.INTERCEPTOR_CLASSES);

        final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        final String deploymentName = deploymentUnit.getName();

        final ServiceName serviceName = deploymentUnit.getServiceName().append("grpc");
        final ServiceBuilder<?> serviceBuilder = phaseContext.getServiceTarget().addService(serviceName);

        final Supplier<WildFlyGrpcDeploymentRegistry> serverServiceSupplier = serviceBuilder
                .requires(GrpcSubsystemDefinition.SERVER_CAPABILITY.getCapabilityServiceName());

        Supplier<BeanManager> beanManagerSupplier = null;
        try {
            final WeldCapability weldCapability = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT)
                    .getOptionalCapabilityRuntimeAPI(Capabilities.WELD_CAPABILITY, WeldCapability.class).orElse(null);
            if (weldCapability == null) {
                LOGGER.debugf("WeldCapability not found for deployment %s", deploymentName);
            } else if (!weldCapability.isPartOfWeldDeployment(deploymentUnit)) {
                LOGGER.debugf("Deployment %s is not a Weld deployment", deploymentName);
            } else {
                beanManagerSupplier = weldCapability.addBeanManagerService(deploymentUnit, serviceBuilder);
                // WeldStartService is installed under the root EAR service name for EAR deployments.
                final DeploymentUnit parent = deploymentUnit.getParent();
                final DeploymentUnit weldRoot = parent != null ? parent : deploymentUnit;
                serviceBuilder.requires(weldRoot.getServiceName().append("WeldStartService"));
                LOGGER.debugf("CDI integration enabled for gRPC services in deployment %s", deploymentName);
            }
        } catch (Exception e) {
            LOGGER.weldCapabilityUnavailable(e, deploymentName);
        }

        final GrpcDeploymentService service = new GrpcDeploymentService(
                deploymentName,
                services,
                interceptorClasses != null ? interceptorClasses : Collections.emptyList(),
                module.getClassLoader(),
                serverServiceSupplier,
                beanManagerSupplier);

        serviceBuilder.setInstance(service);
        serviceBuilder.install();
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }
}

/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.grpc.deployment;

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
import org.wildfly.extension.grpc.WildFlyGrpcDeploymentRegistry;
import org.wildfly.extension.grpc._private.GrpcLogger;

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

        final ServiceName grpcServiceName = ServiceName.parse("org.wildfly.grpc.server");
        final Supplier<WildFlyGrpcDeploymentRegistry> serverServiceSupplier = serviceBuilder
                .requires(grpcServiceName);

        Supplier<BeanManager> beanManagerSupplier = null;
        try {
            final WeldCapability weldCapability = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT)
                    .getOptionalCapabilityRuntimeAPI(Capabilities.WELD_CAPABILITY, WeldCapability.class).orElse(null);
            if (weldCapability == null) {
                GrpcLogger.LOGGER.debug("WeldCapability not found for deployment " + deploymentName);
            } else if (!weldCapability.isPartOfWeldDeployment(deploymentUnit)) {
                GrpcLogger.LOGGER.debug("Deployment " + deploymentName + " is not a Weld deployment");
            } else {
                beanManagerSupplier = weldCapability.addBeanManagerService(deploymentUnit, serviceBuilder);
                serviceBuilder.requires(deploymentUnit.getServiceName().append("WeldStartService"));
                GrpcLogger.LOGGER.debug("CDI integration enabled for gRPC services in deployment " + deploymentName);
            }
        } catch (Exception e) {
            GrpcLogger.LOGGER.warn("Weld capability not available for deployment " + deploymentName, e);
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

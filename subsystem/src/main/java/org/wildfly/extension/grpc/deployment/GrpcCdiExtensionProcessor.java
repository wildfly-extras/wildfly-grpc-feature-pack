/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.grpc.deployment;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.weld.WeldCapability;
import org.wildfly.extension.grpc.Capabilities;
import org.wildfly.extension.grpc.security.GrpcSecurityCdiExtension;

/**
 * Registers the gRPC CDI extension from the subsystem module.
 *
 * <p>This avoids exposing the subsystem module to application deployments merely so Weld can
 * discover a service-provider entry.
 */
public final class GrpcCdiExtensionProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (deploymentUnit.getAttachment(GrpcDeploymentAttachments.GRPC_BINDABLE_SERVICES) == null) {
            return;
        }

        final WeldCapability weldCapability = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT)
                .getOptionalCapabilityRuntimeAPI(Capabilities.WELD_CAPABILITY, WeldCapability.class)
                .orElse(null);
        if (weldCapability != null) {
            weldCapability.registerExtensionInstance(new GrpcSecurityCdiExtension(), deploymentUnit);
        }
    }

    @Override
    public void undeploy(final DeploymentUnit context) {
    }
}

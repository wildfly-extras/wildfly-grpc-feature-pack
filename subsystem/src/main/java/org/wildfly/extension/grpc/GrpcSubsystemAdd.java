/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.grpc;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.CapabilityServiceTarget;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.grpc.deployment.GrpcDependencyProcessor;
import org.wildfly.extension.grpc.deployment.GrpcDeploymentProcessor;
import org.wildfly.extension.grpc.deployment.GrpcServiceInstallProcessor;
import org.wildfly.extension.undertow.Host;

class GrpcSubsystemAdd extends AbstractBoottimeAddStepHandler {

    private static final int DEPENDENCIES_PRIORITY = 6304;
    private static final int POST_MODULE_PRIORITY = 6305;
    private static final int INSTALL_PRIORITY = 6306;

    static GrpcSubsystemAdd INSTANCE = new GrpcSubsystemAdd();

    public GrpcSubsystemAdd() {
        super(GrpcSubsystemDefinition.ATTRIBUTES);
    }

    @Override
    protected void performBoottime(final OperationContext context, final ModelNode operation, final ModelNode model)
            throws OperationFailedException {

        final CapabilityServiceTarget target = context.getCapabilityServiceTarget();
        final CapabilityServiceBuilder<?> builder = target.addCapability(GrpcSubsystemDefinition.SERVER_CAPABILITY);

        final String serverNameRef = GrpcSubsystemDefinition.SERVER_NAME
                .resolveModelAttribute(context, model).asString();
        final String virtualHostRef = GrpcSubsystemDefinition.VIRTUAL_HOST
                .resolveModelAttribute(context, model).asString();
        final Supplier<Host> undertowHost = builder.requiresCapability(
                Capabilities.UNDERTOW_HOST_CAPABILITY, Host.class, serverNameRef, virtualHostRef);

        final int maxInboundMessageSize = GrpcSubsystemDefinition.MAX_INBOUND_MESSAGE_SIZE
                .resolveModelAttribute(context, model).asInt();
        final int maxInboundMetadataSize = GrpcSubsystemDefinition.MAX_INBOUND_METADATA_SIZE
                .resolveModelAttribute(context, model).asInt();

        final Consumer<GrpcUndertowService> provides = builder.provides(GrpcSubsystemDefinition.SERVER_CAPABILITY);

        final GrpcUndertowService service = new GrpcUndertowService(provides, undertowHost,
                maxInboundMessageSize, maxInboundMetadataSize);

        builder.setInstance(service).install();

        context.addStep(new AbstractDeploymentChainStep() {
            public void execute(final DeploymentProcessorTarget processorTarget) {
                processorTarget.addDeploymentProcessor(GrpcExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES,
                        DEPENDENCIES_PRIORITY, new GrpcDependencyProcessor());
                processorTarget.addDeploymentProcessor(GrpcExtension.SUBSYSTEM_NAME, Phase.POST_MODULE,
                        POST_MODULE_PRIORITY, new GrpcDeploymentProcessor());
                processorTarget.addDeploymentProcessor(GrpcExtension.SUBSYSTEM_NAME, Phase.INSTALL,
                        INSTALL_PRIORITY, new GrpcServiceInstallProcessor());
            }
        }, OperationContext.Stage.RUNTIME);
    }
}

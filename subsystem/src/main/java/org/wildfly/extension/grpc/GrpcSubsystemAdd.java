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
import org.wildfly.extension.undertow.Host;

class GrpcSubsystemAdd extends AbstractBoottimeAddStepHandler {

    static GrpcSubsystemAdd INSTANCE = new GrpcSubsystemAdd();

    public GrpcSubsystemAdd() {
        super(GrpcSubsystemDefinition.ATTRIBUTES);
    }

    @Override
    protected void performBoottime(final OperationContext context, final ModelNode operation, final ModelNode model)
            throws OperationFailedException {

        final CapabilityServiceTarget target = context.getCapabilityServiceTarget();
        final CapabilityServiceBuilder<?> builder = target.addCapability(GrpcSubsystemDefinition.SERVER_CAPABILITY);

        final String serverNameRef = GrpcSubsystemDefinition.GRPC_SERVER_NAME
                .resolveModelAttribute(context, model).asString();
        final String virtualHostRef = GrpcSubsystemDefinition.GRPC_VIRTUAL_HOST
                .resolveModelAttribute(context, model).asString();
        final Supplier<Host> undertowHost = builder.requiresCapability(
                Capabilities.UNDERTOW_HOST_CAPABILITY, Host.class, serverNameRef, virtualHostRef);

        final ServerConfiguration configuration = new ServerConfiguration();

        configuration.setMaxInboundMessageSize(
                GrpcSubsystemDefinition.GRPC_MAX_INBOUND_MESSAGE_SIZE.resolveModelAttribute(context, model).asInt());
        configuration.setMaxInboundMetadataSize(
                GrpcSubsystemDefinition.GRPC_MAX_INBOUND_METADATA_SIZE.resolveModelAttribute(context, model).asInt());
        configuration.setShutdownTimeout(
                GrpcSubsystemDefinition.GRPC_SHUTDOWN_TIMEOUT.resolveModelAttribute(context, model).asLong());

        final Consumer<GrpcUndertowService> provides = builder.provides(GrpcSubsystemDefinition.SERVER_CAPABILITY);

        final GrpcUndertowService service = new GrpcUndertowService(provides, undertowHost, configuration.build());

        builder.setInstance(service).install();

        context.addStep(new AbstractDeploymentChainStep() {
            public void execute(final DeploymentProcessorTarget processorTarget) {
                int DEPENDENCIES_PRIORITY = 6304;
                processorTarget.addDeploymentProcessor(GrpcExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES,
                        DEPENDENCIES_PRIORITY, new GrpcDependencyProcessor());

                int DEPLOYMENT_PRIORITY = 6305;
                processorTarget.addDeploymentProcessor(GrpcExtension.SUBSYSTEM_NAME, Phase.POST_MODULE,
                        DEPLOYMENT_PRIORITY, new GrpcDeploymentProcessor(service));
            }
        }, OperationContext.Stage.RUNTIME);
    }
}

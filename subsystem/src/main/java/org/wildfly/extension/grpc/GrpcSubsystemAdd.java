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
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.grpc.deployment.GrpcDependencyProcessor;
import org.wildfly.extension.grpc.deployment.GrpcDeploymentProcessor;
import org.wildfly.extension.grpc.deployment.GrpcServiceInstallProcessor;
import org.wildfly.extension.grpc.deployment.GrpcServletInstallProcessor;
import org.wildfly.extension.undertow.Host;

import static org.jboss.as.server.deployment.Phase.DEPENDENCIES;
import static org.jboss.as.server.deployment.Phase.INSTALL;
import static org.jboss.as.server.deployment.Phase.POST_MODULE;
import static org.wildfly.extension.grpc.GrpcExtension.SUBSYSTEM_NAME;
import static org.wildfly.extension.grpc.GrpcSubsystemDefinition.MAX_INBOUND_MESSAGE_SIZE;
import static org.wildfly.extension.grpc.GrpcSubsystemDefinition.MAX_INBOUND_METADATA_SIZE;
import static org.wildfly.extension.grpc.GrpcSubsystemDefinition.SERVER_CAPABILITY;

class GrpcSubsystemAdd extends AbstractBoottimeAddStepHandler {

    private static final int DEPENDENCIES_PRIORITY = 6304;
    private static final int POST_MODULE_SCAN_PRIORITY = 6305;
    private static final int POST_MODULE_SERVLET_PRIORITY = 6306;
    private static final int INSTALL_PRIORITY = 6306;

    static GrpcSubsystemAdd INSTANCE = new GrpcSubsystemAdd();

    public GrpcSubsystemAdd() {
        super(GrpcSubsystemDefinition.ATTRIBUTES);
    }

    @Override
    protected void performBoottime(final OperationContext context, final ModelNode operation, final ModelNode model)
            throws OperationFailedException {

        final CapabilityServiceTarget target = context.getCapabilityServiceTarget();
        final CapabilityServiceBuilder<?> builder = target.addCapability(SERVER_CAPABILITY);

        final String serverNameRef = GrpcSubsystemDefinition.SERVER_NAME
                .resolveModelAttribute(context, model).asString();
        final String virtualHostRef = GrpcSubsystemDefinition.VIRTUAL_HOST
                .resolveModelAttribute(context, model).asString();
        final Supplier<Host> undertowHost = builder.requiresCapability(
                Capabilities.UNDERTOW_HOST_CAPABILITY, Host.class, serverNameRef, virtualHostRef);

        final Consumer<GrpcUndertowService> provides = builder.provides(SERVER_CAPABILITY);

        final GrpcUndertowService service = new GrpcUndertowService(provides, undertowHost
        );

        builder.setInstance(service).install();

        final int maxInboundMessageSize = MAX_INBOUND_MESSAGE_SIZE
                .resolveModelAttribute(context, model).asInt();
        final int maxInboundMetadataSize = MAX_INBOUND_METADATA_SIZE
                .resolveModelAttribute(context, model).asInt();

        context.addStep(new AbstractDeploymentChainStep() {
            public void execute(final DeploymentProcessorTarget processorTarget) {
                processorTarget.addDeploymentProcessor(SUBSYSTEM_NAME, DEPENDENCIES,
                        DEPENDENCIES_PRIORITY, new GrpcDependencyProcessor());
                processorTarget.addDeploymentProcessor(SUBSYSTEM_NAME, POST_MODULE,
                        POST_MODULE_SCAN_PRIORITY, new GrpcDeploymentProcessor());
                processorTarget.addDeploymentProcessor(SUBSYSTEM_NAME, POST_MODULE,
                        POST_MODULE_SERVLET_PRIORITY,
                        new GrpcServletInstallProcessor(maxInboundMessageSize, maxInboundMetadataSize));
                processorTarget.addDeploymentProcessor(SUBSYSTEM_NAME, INSTALL,
                        INSTALL_PRIORITY, new GrpcServiceInstallProcessor());
            }
        }, OperationContext.Stage.RUNTIME);
    }
}

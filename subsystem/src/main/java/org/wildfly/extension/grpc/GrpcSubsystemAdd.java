/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.grpc;

import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.CapabilityServiceTarget;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.Services;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.grpc.deployment.GrpcDependencyProcessor;
import org.wildfly.extension.grpc.deployment.GrpcDeploymentProcessor;

import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.JdkLoggerFactory;

class GrpcSubsystemAdd extends AbstractBoottimeAddStepHandler {

    static GrpcSubsystemAdd INSTANCE = new GrpcSubsystemAdd();

    public GrpcSubsystemAdd() {
        super(GrpcSubsystemDefinition.ATTRIBUTES);
    }

    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model)
            throws OperationFailedException {
        // Initialize the Netty logger factory
        InternalLoggerFactory.setDefaultFactory(JdkLoggerFactory.INSTANCE);
        // GrpcServerService.configure(operation, context);

        final CapabilityServiceTarget target = context.getCapabilityServiceTarget();
        final CapabilityServiceBuilder<?> builder = target.addCapability(GrpcSubsystemDefinition.SERVER_CAPABILITY);

        final String socketBindingRef = GrpcSubsystemDefinition.GRPC_SERVER_SOCKET_BINDING
                .resolveModelAttribute(context, model).asString();
        Supplier<SocketBinding> socketBinding = builder.requiresCapability(Capabilities.SOCKET_BiNDING,
                SocketBinding.class, socketBindingRef);

        final ServerConfiguration configuration = new ServerConfiguration(socketBinding);

        configuration.setFlowControlWindow(
                GrpcSubsystemDefinition.GRPC_FLOW_CONTROL_WINDOW.resolveModelAttribute(context, model).asInt());
        configuration.setHandshakeTimeout(
                GrpcSubsystemDefinition.GRPC_HANDSHAKE_TIMEOUT.resolveModelAttribute(context, model).asInt());
        configuration.setInitialFlowControlWindow(
                GrpcSubsystemDefinition.GRPC_INITIAL_FLOW_CONTROL_WINDOW.resolveModelAttribute(context, model).asInt());
        configuration.setKeepLiveTime(
                GrpcSubsystemDefinition.GRPC_KEEP_ALIVE_TIME.resolveModelAttribute(context, model).asLong(-1));
        configuration.setKeepAliveTimeout(
                GrpcSubsystemDefinition.GRPC_KEEP_ALIVE_TIMEOUT.resolveModelAttribute(context, model).asLong(-1));
        configuration
                .setMaxConcurrentCallsPerConnection(GrpcSubsystemDefinition.GRPC_MAX_CONCURRENT_CALLS_PER_CONNECTION
                        .resolveModelAttribute(context, model).asInt(-1));
        configuration.setMaxConnectionAge(
                GrpcSubsystemDefinition.GRPC_MAX_CONNECTION_AGE.resolveModelAttribute(context, model).asLong(-1));
        configuration.setMaxConnectionAgeGrace(
                GrpcSubsystemDefinition.GRPC_MAX_CONNECTION_AGE_GRACE.resolveModelAttribute(context, model).asLong(-1));
        configuration.setMaxConnectionIdle(
                GrpcSubsystemDefinition.GRPC_MAX_CONNECTION_IDLE.resolveModelAttribute(context, model).asLong(-1));
        configuration.setMaxInboundMessageSize(
                GrpcSubsystemDefinition.GRPC_MAX_INBOUND_MESSAGE_SIZE.resolveModelAttribute(context, model).asInt());
        configuration.setMaxInboundMetadataSize(
                GrpcSubsystemDefinition.GRPC_MAX_INBOUND_METADATA_SIZE.resolveModelAttribute(context, model).asInt());
        configuration.setPermitKeepAliveTime(
                GrpcSubsystemDefinition.GRPC_PERMIT_KEEP_ALIVE_TIME.resolveModelAttribute(context, model).asLong(-1));
        configuration.setPermitKeepAliveWithoutCalls(GrpcSubsystemDefinition.GRPC_PERMIT_KEEP_ALIVE_WITHOUT_CALLS
                .resolveModelAttribute(context, model).asBoolean());

        configuration
                .setProtocolProvider(GrpcSubsystemDefinition.GRPC_PROTOCOL_PROVIDER
                        .resolveModelAttribute(context, model).asStringOrNull())
                .setSessionCacheSize(GrpcSubsystemDefinition.GRPC_SESSION_CACHE_SIZE
                        .resolveModelAttribute(context, model).asLongOrNull())
                .setSessionTimeout(GrpcSubsystemDefinition.GRPC_SESSION_TIMEOUT.resolveModelAttribute(context, model)
                        .asLongOrNull())
                .setShutdownTimeout(
                        GrpcSubsystemDefinition.GRPC_SHUTDOWN_TIMEOUT.resolveModelAttribute(context, model).asInt())
                .setStartTls(GrpcSubsystemDefinition.GRPC_START_TLS.resolveModelAttribute(context, model).asBoolean());

        if (isDefined(GrpcSubsystemDefinition.GRPC_TRUST_MANAGER_NAME, model)) {
            configuration.setTrustManager(builder.requiresCapability(Capabilities.TRUST_MANAGER_CAPABILITY,
                    TrustManager.class,
                    GrpcSubsystemDefinition.GRPC_TRUST_MANAGER_NAME.resolveModelAttribute(context, model).asString()));
        }

        if (isDefined(GrpcSubsystemDefinition.GRPC_KEY_MANAGER_NAME, model)) {
            final String name = GrpcSubsystemDefinition.GRPC_KEY_MANAGER_NAME.resolveModelAttribute(context, model)
                    .asString();
            if (!name.isBlank()) {
                configuration.setKeyManager(
                        builder.requiresCapability(Capabilities.KEY_MANAGER_CAPABILITY, KeyManager.class, name));
            }
        }

        if (isDefined(GrpcSubsystemDefinition.GRPC_SSL_CONTEXT_NAME, model)) {
            configuration.setSslContext(builder.requiresCapability(Capabilities.SSL_CONTEXT_CAPABILITY,
                    SSLContext.class,
                    GrpcSubsystemDefinition.GRPC_SSL_CONTEXT_NAME.resolveModelAttribute(context, model).asString()));
        }

        final Consumer<GrpcServerService> provides = builder.provides(GrpcSubsystemDefinition.SERVER_CAPABILITY);

        final GrpcServerService service = new GrpcServerService(provides, Services.requireServerExecutor(builder),
                configuration.build());

        builder.setInstance(service).install();

        context.addStep(new AbstractDeploymentChainStep() {
            public void execute(DeploymentProcessorTarget processorTarget) {
                // TODO What phases and priorities should I use?
                int DEPENDENCIES_PRIORITY = 6304;
                processorTarget.addDeploymentProcessor(GrpcExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES,
                        DEPENDENCIES_PRIORITY, new GrpcDependencyProcessor());

                int DEPLOYMENT_PRIORITY = 6305;
                processorTarget.addDeploymentProcessor(GrpcExtension.SUBSYSTEM_NAME, Phase.POST_MODULE,
                        DEPLOYMENT_PRIORITY, new GrpcDeploymentProcessor(service));
            }
        }, OperationContext.Stage.RUNTIME);

    }

    private static boolean isDefined(final AttributeDefinition def, final ModelNode model) {
        return model.hasDefined(def.getName());
    }

}

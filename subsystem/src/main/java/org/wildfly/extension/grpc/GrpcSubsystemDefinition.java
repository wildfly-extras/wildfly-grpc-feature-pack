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
package org.wildfly.extension.grpc;

import java.util.Collection;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.LongRangeValidator;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

public class GrpcSubsystemDefinition extends PersistentResourceDefinition {

    static final SimpleAttributeDefinition GRPC_FLOW_CONTROL_WINDOW = new SimpleAttributeDefinitionBuilder(
            "flow-control-window", ModelType.INT)
            .setAllowExpression(true)
            .setRequired(false)
            .setRestartAllServices()
            .setValidator(new IntRangeValidator(0, false, true))
            .build();

    static final SimpleAttributeDefinition GRPC_HANDSHAKE_TIMEOUT = new SimpleAttributeDefinitionBuilder(
            "handshake-timeout", ModelType.LONG)
            .setAllowExpression(true)
            .setRequired(false)
            .setRestartAllServices()
            .setValidator(new IntRangeValidator(0, false, true))
            .build();

    static final SimpleAttributeDefinition GRPC_INITIAL_FLOW_CONTROL_WINDOW = new SimpleAttributeDefinitionBuilder(
            "initial-flow-control-window", ModelType.INT)
            .setAllowExpression(true)
            .setRequired(false)
            .setRestartAllServices()
            .setValidator(new IntRangeValidator(0, false, true))
            .build();

    static final SimpleAttributeDefinition GRPC_KEEP_ALIVE_TIME = new SimpleAttributeDefinitionBuilder(
            "keep-alive-time", ModelType.LONG)
            .setAllowExpression(true)
            .setRequired(false)
            .setRestartAllServices()
            .setValidator(new IntRangeValidator(0, false, true))
            .build();

    static final SimpleAttributeDefinition GRPC_KEEP_ALIVE_TIMEOUT = new SimpleAttributeDefinitionBuilder(
            "keep-alive-timeout", ModelType.LONG)
            .setAllowExpression(true)
            .setRequired(false)
            .setRestartAllServices()
            .setValidator(new IntRangeValidator(0, false, true))
            .build();

    static final SimpleAttributeDefinition GRPC_KEY_MANAGER_NAME = new SimpleAttributeDefinitionBuilder(
            "key-manager-name", ModelType.STRING)
            .setAllowExpression(true)
            .setCapabilityReference(Capabilities.KEY_MANAGER_CAPABILITY)
            .setRequired(false)
            .setRestartAllServices()
            .setValidator(new ModelTypeValidator(ModelType.STRING, false))
            .build();

    static final SimpleAttributeDefinition GRPC_MAX_CONCURRENT_CALLS_PER_CONNECTION = new SimpleAttributeDefinitionBuilder(
            "max-concurrent-calls-per-connection", ModelType.INT)
            .setAllowExpression(true)
            .setRequired(false)
            .setRestartAllServices()
            .setValidator(new IntRangeValidator(0, false, true))
            .build();

    static final SimpleAttributeDefinition GRPC_MAX_CONNECTION_AGE = new SimpleAttributeDefinitionBuilder(
            "max-connection-age", ModelType.LONG)
            .setAllowExpression(true)
            .setRequired(false)
            .setRestartAllServices()
            .setValidator(new IntRangeValidator(0, false, true))
            .build();

    static final SimpleAttributeDefinition GRPC_MAX_CONNECTION_AGE_GRACE = new SimpleAttributeDefinitionBuilder(
            "max-connection-age-grace", ModelType.LONG)
            .setAllowExpression(true)
            .setRequired(false)
            .setRestartAllServices()
            .setValidator(new IntRangeValidator(0, false, true))
            .build();

    static final SimpleAttributeDefinition GRPC_MAX_CONNECTION_IDLE = new SimpleAttributeDefinitionBuilder(
            "max-connection-idle", ModelType.LONG)
            .setAllowExpression(true)
            .setRequired(false)
            .setRestartAllServices()
            .setValidator(new IntRangeValidator(0, false, true))
            .build();

    static final SimpleAttributeDefinition GRPC_MAX_INBOUND_MESSAGE_SIZE = new SimpleAttributeDefinitionBuilder(
            "max-inbound-message-size", ModelType.INT)
            .setAllowExpression(true)
            .setRequired(false)
            .setRestartAllServices()
            .setValidator(new IntRangeValidator(0, false, true))
            .build();

    static final SimpleAttributeDefinition GRPC_MAX_INBOUND_METADATA_SIZE = new SimpleAttributeDefinitionBuilder(
            "max-inbound-metadata-size", ModelType.INT)
            .setAllowExpression(true)
            .setRequired(false)
            .setRestartAllServices()
            .setValidator(new IntRangeValidator(0, false, true))
            .build();

    static final SimpleAttributeDefinition GRPC_PERMIT_KEEP_ALIVE_TIME = new SimpleAttributeDefinitionBuilder(
            "permit-keep-alive-time", ModelType.LONG)
            .setAllowExpression(true)
            .setRequired(false)
            .setRestartAllServices()
            .setValidator(new IntRangeValidator(0, false, true))
            .build();

    static final SimpleAttributeDefinition GRPC_PERMIT_KEEP_ALIVE_WITHOUT_CALLS = new SimpleAttributeDefinitionBuilder(
            "permit-keep-alive-without-calls", ModelType.BOOLEAN)
            .setAllowExpression(true)
            .setDefaultValue(ModelNode.FALSE)
            .setRequired(false)
            .setRestartAllServices()
            .setValidator(new ModelTypeValidator(ModelType.BOOLEAN, false))
            .build();

    static final SimpleAttributeDefinition GRPC_PROTOCOL_PROVIDER = new SimpleAttributeDefinitionBuilder(
            "protocol-provider", ModelType.STRING)
            .setAllowExpression(true)
            .setRequired(false)
            .setRestartAllServices()
            .setValidator(new ModelTypeValidator(ModelType.STRING, true))
            .build();

    static final SimpleAttributeDefinition GRPC_SERVER_HOST = new SimpleAttributeDefinitionBuilder(
            "server-host", ModelType.STRING)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode("localhost"))
            .setRequired(false)
            .setRestartAllServices()
            .setValidator(new ModelTypeValidator(ModelType.STRING, true))
            .build();

    static final SimpleAttributeDefinition GRPC_SERVER_PORT = new SimpleAttributeDefinitionBuilder(
            "server-port", ModelType.LONG)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(9555))
            .setRequired(false)
            .setRestartAllServices()
            .setValidator(new IntRangeValidator(0, 65535, true, true))
            .build();

    static final SimpleAttributeDefinition GRPC_SESSION_CACHE_SIZE = new SimpleAttributeDefinitionBuilder(
            "session-cache-size", ModelType.LONG)
            .setAllowExpression(true)
            .setRequired(false)
            .setRestartAllServices()
            .setValidator(new LongRangeValidator(0, Long.MAX_VALUE, true, true))
            .build();

    static final SimpleAttributeDefinition GRPC_SESSION_TIMEOUT = new SimpleAttributeDefinitionBuilder(
            "session-timeout", ModelType.LONG)
            .setAllowExpression(true)
            .setRequired(false)
            .setRestartAllServices()
            .setValidator(new LongRangeValidator(0, Long.MAX_VALUE, true, true))
            .build();

    static final SimpleAttributeDefinition GRPC_SHUTDOWN_TIMEOUT = new SimpleAttributeDefinitionBuilder(
            "shutdown-timeout", ModelType.LONG)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(3L))
            .setRequired(false)
            .setRestartAllServices()
            .setValidator(new LongRangeValidator(0, Integer.MAX_VALUE, true, true))
            .build();

    static final SimpleAttributeDefinition GRPC_SSL_CONTEXT_NAME = new SimpleAttributeDefinitionBuilder(
            "ssl-context-name", ModelType.STRING)
            .setAllowExpression(true)
            .setCapabilityReference(Capabilities.SSL_CONTEXT_CAPABILITY)
            .setRequired(false)
            .setRestartAllServices()
            .setValidator(new ModelTypeValidator(ModelType.STRING, true))
            .build();

    static final SimpleAttributeDefinition GRPC_START_TLS = new SimpleAttributeDefinitionBuilder(
            "start-tls", ModelType.BOOLEAN)
            .setAllowExpression(true)
            .setDefaultValue(ModelNode.FALSE)
            .setRequired(false)
            .setRestartAllServices()
            .setValidator(new ModelTypeValidator(ModelType.BOOLEAN, true))
            .build();

    static final SimpleAttributeDefinition GRPC_TRUST_MANAGER_NAME = new SimpleAttributeDefinitionBuilder(
            "trust-manager-name", ModelType.STRING)
            .setAllowExpression(true)
            .setCapabilityReference(Capabilities.TRUST_MANAGER_CAPABILITY)
            .setRequired(false)
            .setRestartAllServices()
            .setValidator(new ModelTypeValidator(ModelType.STRING, true))
            .build();

    static final List<AttributeDefinition> ATTRIBUTES = List.of(
            GRPC_FLOW_CONTROL_WINDOW,
            GRPC_HANDSHAKE_TIMEOUT,
            GRPC_INITIAL_FLOW_CONTROL_WINDOW,
            GRPC_KEEP_ALIVE_TIME,
            GRPC_KEEP_ALIVE_TIMEOUT,
            GRPC_KEY_MANAGER_NAME,
            GRPC_MAX_CONCURRENT_CALLS_PER_CONNECTION,
            GRPC_MAX_CONNECTION_AGE,
            GRPC_MAX_CONNECTION_AGE_GRACE,
            GRPC_MAX_CONNECTION_IDLE,
            GRPC_MAX_INBOUND_MESSAGE_SIZE,
            GRPC_MAX_INBOUND_METADATA_SIZE,
            GRPC_PERMIT_KEEP_ALIVE_TIME,
            GRPC_PERMIT_KEEP_ALIVE_WITHOUT_CALLS,
            GRPC_PROTOCOL_PROVIDER,
            GRPC_SERVER_HOST,
            GRPC_SERVER_PORT,
            GRPC_SESSION_CACHE_SIZE,
            GRPC_SESSION_TIMEOUT,
            GRPC_SHUTDOWN_TIMEOUT,
            GRPC_SSL_CONTEXT_NAME,
            GRPC_START_TLS,
            GRPC_TRUST_MANAGER_NAME);

    static RuntimeCapability<Void> SERVER_CAPABILITY = RuntimeCapability.Builder.of("org.wildfly.grpc.server", false)
            .setServiceType(GrpcServerService.class)
            .build();

    // This must be initialized last to ensure the other static attributes are created first
    static final GrpcSubsystemDefinition INSTANCE = new GrpcSubsystemDefinition();

    public GrpcSubsystemDefinition() {
        super(new SimpleResourceDefinition.Parameters(Paths.SUBSYSTEM, GrpcExtension.getResolver())
                .addCapabilities(SERVER_CAPABILITY)
                .setAddHandler(GrpcSubsystemAdd.INSTANCE)
                .setRemoveHandler(ReloadRequiredRemoveStepHandler.INSTANCE));
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return ATTRIBUTES;
    }
}

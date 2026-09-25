/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.grpc;

import static org.wildfly.extension.grpc.GrpcSubsystemRegistrar.RESOLVER;

import java.util.Collection;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.LongRangeValidator;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

public class GrpcSubsystemDefinition extends PersistentResourceDefinition {

    static final SimpleAttributeDefinition GRPC_KEEP_ALIVE_TIME = new SimpleAttributeDefinitionBuilder(
            "keep-alive-time", ModelType.LONG).setAllowExpression(true).setMeasurementUnit(MeasurementUnit.SECONDS)
            .setRequired(false).setRestartAllServices().setValidator(new IntRangeValidator(0, false, true))
            .build();

    static final SimpleAttributeDefinition GRPC_KEEP_ALIVE_TIMEOUT = new SimpleAttributeDefinitionBuilder(
            "keep-alive-timeout", ModelType.LONG).setAllowExpression(true).setMeasurementUnit(MeasurementUnit.SECONDS)
            .setRequired(false).setRestartAllServices().setValidator(new IntRangeValidator(0, false, true))
            .build();

    static final SimpleAttributeDefinition GRPC_MAX_CONCURRENT_CALLS_PER_CONNECTION = new SimpleAttributeDefinitionBuilder(
            "max-concurrent-calls-per-connection", ModelType.INT).setAllowExpression(true).setRequired(false)
            .setRestartAllServices().setValidator(new IntRangeValidator(0, false, true)).build();

    static final SimpleAttributeDefinition GRPC_MAX_CONNECTION_AGE = new SimpleAttributeDefinitionBuilder(
            "max-connection-age", ModelType.LONG).setAllowExpression(true).setMeasurementUnit(MeasurementUnit.SECONDS)
            .setRequired(false).setRestartAllServices().setValidator(new IntRangeValidator(0, false, true))
            .build();

    static final SimpleAttributeDefinition GRPC_MAX_CONNECTION_AGE_GRACE = new SimpleAttributeDefinitionBuilder(
            "max-connection-age-grace", ModelType.LONG).setAllowExpression(true)
            .setMeasurementUnit(MeasurementUnit.SECONDS).setRequired(false).setRestartAllServices()
            .setValidator(new IntRangeValidator(0, false, true)).build();

    static final SimpleAttributeDefinition GRPC_MAX_CONNECTION_IDLE = new SimpleAttributeDefinitionBuilder(
            "max-connection-idle", ModelType.LONG).setAllowExpression(true).setMeasurementUnit(MeasurementUnit.SECONDS)
            .setRequired(false).setRestartAllServices().setValidator(new IntRangeValidator(0, false, true))
            .build();

    static final SimpleAttributeDefinition GRPC_MAX_INBOUND_MESSAGE_SIZE = new SimpleAttributeDefinitionBuilder(
            "max-inbound-message-size", ModelType.INT).setAllowExpression(true).setDefaultValue(new ModelNode(4194304))
            .setMeasurementUnit(MeasurementUnit.BYTES).setRequired(false).setRestartAllServices()
            .setValidator(new IntRangeValidator(0, false, true)).build();

    static final SimpleAttributeDefinition GRPC_MAX_INBOUND_METADATA_SIZE = new SimpleAttributeDefinitionBuilder(
            "max-inbound-metadata-size", ModelType.INT).setAllowExpression(true).setDefaultValue(new ModelNode(8192))
            .setMeasurementUnit(MeasurementUnit.BYTES).setRequired(false).setRestartAllServices()
            .setValidator(new IntRangeValidator(0, false, true)).build();

    static final SimpleAttributeDefinition GRPC_PERMIT_KEEP_ALIVE_TIME = new SimpleAttributeDefinitionBuilder(
            "permit-keep-alive-time", ModelType.LONG).setAllowExpression(true).setDefaultValue(new ModelNode(250))
            .setMeasurementUnit(MeasurementUnit.SECONDS).setRequired(false).setRestartAllServices()
            .setValidator(new IntRangeValidator(0, false, true)).build();

    static final SimpleAttributeDefinition GRPC_PERMIT_KEEP_ALIVE_WITHOUT_CALLS = new SimpleAttributeDefinitionBuilder(
            "permit-keep-alive-without-calls", ModelType.BOOLEAN).setAllowExpression(true)
            .setDefaultValue(ModelNode.FALSE).setRequired(false).setRestartAllServices()
            .setValidator(new ModelTypeValidator(ModelType.BOOLEAN, false)).build();

    static final SimpleAttributeDefinition GRPC_SHUTDOWN_TIMEOUT = new SimpleAttributeDefinitionBuilder(
            "shutdown-timeout", ModelType.LONG).setAllowExpression(true).setDefaultValue(new ModelNode(3L))
            .setRequired(false).setRestartAllServices()
            .setValidator(new LongRangeValidator(0, Integer.MAX_VALUE, true, true)).build();

    static final SimpleAttributeDefinition GRPC_SERVER_NAME = new SimpleAttributeDefinitionBuilder(
            "server-name", ModelType.STRING).setAllowExpression(false).setRequired(false)
            .setDefaultValue(new ModelNode("default-server")).setRestartAllServices()
            .setValidator(new ModelTypeValidator(ModelType.STRING, true)).build();

    static final SimpleAttributeDefinition GRPC_VIRTUAL_HOST = new SimpleAttributeDefinitionBuilder(
            "virtual-host", ModelType.STRING).setAllowExpression(false).setRequired(false)
            .setDefaultValue(new ModelNode("default-host")).setRestartAllServices()
            .setValidator(new ModelTypeValidator(ModelType.STRING, true)).build();

    static final List<AttributeDefinition> ATTRIBUTES = List.of(GRPC_KEEP_ALIVE_TIME, GRPC_KEEP_ALIVE_TIMEOUT,
            GRPC_MAX_CONCURRENT_CALLS_PER_CONNECTION, GRPC_MAX_CONNECTION_AGE, GRPC_MAX_CONNECTION_AGE_GRACE,
            GRPC_MAX_CONNECTION_IDLE, GRPC_MAX_INBOUND_MESSAGE_SIZE, GRPC_MAX_INBOUND_METADATA_SIZE,
            GRPC_PERMIT_KEEP_ALIVE_TIME, GRPC_PERMIT_KEEP_ALIVE_WITHOUT_CALLS, GRPC_SERVER_NAME,
            GRPC_SHUTDOWN_TIMEOUT, GRPC_VIRTUAL_HOST);

    static RuntimeCapability<Void> SERVER_CAPABILITY = RuntimeCapability.Builder.of("org.wildfly.grpc.server", false)
            .setServiceType(GrpcUndertowService.class).build();

    // This must be initialized last to ensure the other static attributes are created first
    static final GrpcSubsystemDefinition INSTANCE = new GrpcSubsystemDefinition();

    public GrpcSubsystemDefinition() {
        super(new SimpleResourceDefinition.Parameters(Paths.SUBSYSTEM, RESOLVER).addCapabilities(SERVER_CAPABILITY)
                .setAddHandler(GrpcSubsystemAdd.INSTANCE).setRemoveHandler(ReloadRequiredRemoveStepHandler.INSTANCE));
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return ATTRIBUTES;
    }
}

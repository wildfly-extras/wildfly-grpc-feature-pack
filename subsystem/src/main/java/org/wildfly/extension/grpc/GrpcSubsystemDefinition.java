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
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

public class GrpcSubsystemDefinition extends PersistentResourceDefinition {

    static final SimpleAttributeDefinition GRPC_SERVER_HOST = new SimpleAttributeDefinitionBuilder(
            "server-host", ModelType.STRING)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode("0.0.0.0"))
            .setRequired(false)
            .setRestartAllServices()
            .setValidator(new ModelTypeValidator(ModelType.STRING, false))
            .build();

    static final SimpleAttributeDefinition GRPC_SERVER_PORT = new SimpleAttributeDefinitionBuilder(
            "server-port", ModelType.INT)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(9555))
            .setRequired(false)
            .setRestartAllServices()
            .setValidator(new IntRangeValidator(0, 65535, false, true))
            .build();

    static final List<AttributeDefinition> ATTRIBUTES = List.of(
            GRPC_SERVER_HOST,
            GRPC_SERVER_PORT);

    // This must be initialized last to ensure the other static attributes are created first
    static final GrpcSubsystemDefinition INSTANCE = new GrpcSubsystemDefinition();

    public GrpcSubsystemDefinition() {
        super(new SimpleResourceDefinition.Parameters(Paths.SUBSYSTEM, GrpcExtension.getResolver())
                .setAddHandler(GrpcSubsystemAdd.INSTANCE)
                .setRemoveHandler(ReloadRequiredRemoveStepHandler.INSTANCE));
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return ATTRIBUTES;
    }
}

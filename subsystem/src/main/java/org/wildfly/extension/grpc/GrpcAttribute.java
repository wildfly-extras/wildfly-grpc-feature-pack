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

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * gRPC configuration attributes
 */
public abstract class GrpcAttribute {

    static final String GRPC_PARAMETER_GROUP = "gRPC";

    static final SimpleAttributeDefinition GRPC_SERVER_HOST = new SimpleAttributeDefinitionBuilder(
            GrpcConstants.GRPC_SERVER_HOST, ModelType.STRING)
            .setRequired(false)
            .setAllowExpression(true)
            .setValidator(new ModelTypeValidator(ModelType.STRING, false))
            .setDefaultValue(new ModelNode("0.0.0.0"))
            .setAttributeGroup(GRPC_PARAMETER_GROUP)
            .build();

    static final SimpleAttributeDefinition GRPC_SERVER_PORT = new SimpleAttributeDefinitionBuilder(
            GrpcConstants.GRPC_SERVER_PORT, ModelType.INT)
            .setRequired(false)
            .setAllowExpression(true)
            .setValidator(new ModelTypeValidator(ModelType.INT, false))
            .setDefaultValue(new ModelNode(9555))
            .setAttributeGroup(GRPC_PARAMETER_GROUP)
            .build();

    static final AttributeDefinition[] ATTRIBUTES = new AttributeDefinition[] {
            GRPC_SERVER_HOST,
            GRPC_SERVER_PORT,
    };
}

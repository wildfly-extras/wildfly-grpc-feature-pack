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

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;

class GrpcServiceDefinition extends SimpleResourceDefinition {

    static final GrpcServiceDefinition INSTANCE = new GrpcServiceDefinition();

    static final SimpleAttributeDefinition SERVICE_CLASS = new SimpleAttributeDefinitionBuilder("service-class",
            ModelType.STRING, false).setStorageRuntime().build();

    public GrpcServiceDefinition() {
        super(Paths.GRPC_SERVICE, GrpcExtension.getResolver("deployment.grpc-service"));
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadOnlyAttribute(SERVICE_CLASS, null);
    }
}

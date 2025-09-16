/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.grpc;

import static org.wildfly.extension.grpc.GrpcSubsystemRegistrar.RESOLVER;

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ParentResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;

class GrpcServiceDefinition extends SimpleResourceDefinition {

    static final ParentResourceDescriptionResolver SERVICE_RESOLVER = RESOLVER.createChildResolver("deployment")
            .createChildResolver("grpc-service");

    static final GrpcServiceDefinition INSTANCE = new GrpcServiceDefinition();

    static final SimpleAttributeDefinition SERVICE_CLASS = new SimpleAttributeDefinitionBuilder("service-class",
            ModelType.STRING, false).setStorageRuntime().build();

    public GrpcServiceDefinition() {
        super(Paths.GRPC_SERVICE, SERVICE_RESOLVER);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadOnlyAttribute(SERVICE_CLASS, null);
    }
}

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

import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ParentResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.SubsystemResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.SubsystemResourceDefinitionRegistrar;

import static org.wildfly.extension.grpc.GrpcExtension.SUBSYSTEM_NAME;

/**
 * Resource registrar for the root resource of the GRPC subsystem.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
class GrpcSubsystemRegistrar implements SubsystemResourceDefinitionRegistrar {

    static final ParentResourceDescriptionResolver RESOLVER = new SubsystemResourceDescriptionResolver(SUBSYSTEM_NAME,
            GrpcSubsystemRegistrar.class);

    @Override
    public ManagementResourceRegistration register(SubsystemRegistration parent,
            ManagementResourceRegistrationContext managementResourceRegistrationContext) {
        ManagementResourceRegistration registration = parent.registerSubsystemModel(GrpcSubsystemDefinition.INSTANCE);

        // TODO - Need to move DUPs here but can they stay in their add handler for now?

        // /deployment=*/subsystem=grpc
        if (managementResourceRegistrationContext.isRuntimeOnlyRegistrationValid()) {
            ManagementResourceRegistration deployment = parent
                    .registerDeploymentModel(GrpcDeploymentDefinition.INSTANCE);
            deployment.registerSubModel(GrpcServiceDefinition.INSTANCE);
        }

        return registration;
    }

}

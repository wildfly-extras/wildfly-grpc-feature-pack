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

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

public class GrpcExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "grpc";
    public static final ModelVersion VERSION_1_0_0 = ModelVersion.create(1, 0, 0);
    public static final ModelVersion CURRENT_MODEL_VERSION = VERSION_1_0_0;
    private static final String RESOURCE_NAME = GrpcExtension.class.getPackage().getName() + ".LocalDescriptions";

    static StandardResourceDescriptionResolver getResolver(final String... keyPrefix) {
        StringBuilder prefix = new StringBuilder(SUBSYSTEM_NAME);
        for (String kp : keyPrefix) {
            prefix.append('.').append(kp);
        }
        return new StandardResourceDescriptionResolver(prefix.toString(), RESOURCE_NAME,
                GrpcExtension.class.getClassLoader(), true, false);
    }

    @Override
    public void initialize(ExtensionContext extensionContext) {
        SubsystemRegistration sr = extensionContext.registerSubsystem(SUBSYSTEM_NAME, CURRENT_MODEL_VERSION);
        sr.registerXMLElementWriter(GrpcSubsystemParser_1_0::new);

        // /subsystem=grpc
        ManagementResourceRegistration subsystem = sr.registerSubsystemModel(GrpcSubsystemDefinition.INSTANCE);
        subsystem.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION,
                GenericSubsystemDescribeHandler.INSTANCE, false);

        // /deployment=*/subsystem=grpc
        ManagementResourceRegistration deployment = sr.registerDeploymentModel(GrpcDeploymentDefinition.INSTANCE);
        deployment.registerSubModel(GrpcServiceDefinition.INSTANCE);
    }

    @Override
    public void initializeParsers(ExtensionParsingContext extensionParsingContext) {
        extensionParsingContext.setSubsystemXmlMapping(SUBSYSTEM_NAME, GrpcSubsystemParser_1_0.NAMESPACE,
                GrpcSubsystemParser_1_0::new);
    }
}

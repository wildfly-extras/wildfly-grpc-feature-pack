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

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.extension.grpc.deployment.GrpcDependencyProcessor;
import org.wildfly.extension.grpc.deployment.GrpcDeploymentProcessor;

class GrpcSubsystemAdd extends AbstractBoottimeAddStepHandler {

    static GrpcSubsystemAdd INSTANCE = new GrpcSubsystemAdd();

    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model)
            throws OperationFailedException {
        ServiceTarget serviceTarget = context.getServiceTarget();
        ServiceBuilder<?> builder = serviceTarget.addService(GrpcSubsystemService.SERVICE_NAME);
        builder.setInstance(new GrpcSubsystemService());
        builder.install();

        context.addStep(new AbstractDeploymentChainStep() {
            public void execute(DeploymentProcessorTarget processorTarget) {
                // TODO What phases and priorities should I use?
                int DEPENDENCIES_PRIORITY = 6304;
                processorTarget.addDeploymentProcessor(GrpcExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES,
                        DEPENDENCIES_PRIORITY, new GrpcDependencyProcessor());

                int DEPLOYMENT_PRIORITY = 6305;
                processorTarget.addDeploymentProcessor(GrpcExtension.SUBSYSTEM_NAME, Phase.POST_MODULE,
                        DEPLOYMENT_PRIORITY, new GrpcDeploymentProcessor());
            }
        }, OperationContext.Stage.RUNTIME);

        GrpcServerConfig serverConfig = createServerConfig(operation, context);
        GrpcServerConfigService.install(serviceTarget, serverConfig);
    }

    private static GrpcServerConfig createServerConfig(ModelNode configuration, OperationContext context)
            throws OperationFailedException {
        final GrpcServerConfig config = new GrpcServerConfig();
        if (configuration.hasDefined(GrpcConstants.GRPC_SERVER_HOST)) {
            config.setWildflyGrpcServerHost(
                    GrpcAttribute.GRPC_SERVER_HOST.resolveModelAttribute(context, configuration));
        }
        if (configuration.hasDefined(GrpcConstants.GRPC_SERVER_PORT)) {
            config.setWildflyGrpcServerPort(
                    GrpcAttribute.GRPC_SERVER_PORT.resolveModelAttribute(context, configuration));
        }
        return config;
    }
}

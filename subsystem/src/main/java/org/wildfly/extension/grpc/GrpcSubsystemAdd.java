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
import org.wildfly.extension.grpc.deployment.GrpcDependencyProcessor;
import org.wildfly.extension.grpc.deployment.GrpcDeploymentProcessor;

class GrpcSubsystemAdd extends AbstractBoottimeAddStepHandler {

    static GrpcSubsystemAdd INSTANCE = new GrpcSubsystemAdd();

    public GrpcSubsystemAdd() {
        super(GrpcSubsystemDefinition.ATTRIBUTES);
    }

    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model)
            throws OperationFailedException {

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

    }
}

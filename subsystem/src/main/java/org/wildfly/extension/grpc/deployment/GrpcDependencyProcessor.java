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
package org.wildfly.extension.grpc.deployment;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;

public class GrpcDependencyProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        addModuleDependencies(deploymentUnit);
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

    private void addModuleDependencies(DeploymentUnit deploymentUnit) {
        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        final ModuleLoader moduleLoader = Module.getBootModuleLoader();

        // Pull in dependencies needed by deployments in the subsystem
        moduleSpecification
                .addSystemDependency(new ModuleDependency(moduleLoader, "org.jboss.jandex", false, false, true, false));

        // This is needed if running with a security manager, and seems to be needed by arquillian in all cases
        moduleSpecification.addSystemDependency(
                new ModuleDependency(moduleLoader, "org.wildfly.security.manager", false, false, true, false));

        moduleSpecification.addSystemDependency(cdiDependency(
                new ModuleDependency(moduleLoader, "org.wildfly.grpc-dependency", false, false, true, false)));
    }

    private ModuleDependency cdiDependency(ModuleDependency moduleDependency) {
        // This is needed following https://issues.redhat.com/browse/WFLY-13641 /
        // https://github.com/wildfly/wildfly/pull/13406
        moduleDependency.addImportFilter(s -> s.equals("META-INF"), true);
        return moduleDependency;
    }
}

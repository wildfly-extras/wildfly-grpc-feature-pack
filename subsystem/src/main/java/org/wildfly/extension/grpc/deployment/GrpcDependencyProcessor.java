/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
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
import org.jboss.modules.filter.PathFilters;

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
        // moduleSpecification
        // .addSystemDependency(new ModuleDependency(moduleLoader, "org.jboss.jandex", false, false, true, false));

        // This is needed if running with a security manager, and seems to be needed by arquillian in all cases
        moduleSpecification.addSystemDependency(
                new ModuleDependency(moduleLoader, "org.wildfly.security.manager", false, false, true, false));

        moduleSpecification.addSystemDependency(cdiDependency(
                new ModuleDependency(moduleLoader, "org.wildfly.grpc-dependency", false, false, true, false)));

        // Expose the gRPC subsystem's public API package to user deployments.
        // GrpcSecurity.SECURITY_IDENTITY_KEY must be loaded from the subsystem ClassLoader
        // so that the same Context.Key instance is shared between the built-in interceptor
        // and service code that reads from the gRPC Context.
        // Expose: org.wildfly.extension.grpc package (GrpcSecurity, SECURITY_IDENTITY_KEY) AND
        // META-INF/services (so Weld discovers GrpcSecurityCdiExtension which registers the
        // Elytron-backed SecurityContext bean — enabling @RolesAllowed to work without custom code).
        // importServices=true loads the extension ServiceLoader entries from the module.
        final ModuleDependency grpcSubsystem = new ModuleDependency(
                moduleLoader, "org.wildfly.extension.grpc", false, false, true, false);
        grpcSubsystem.addImportFilter(PathFilters.isOrIsChildOf("org/wildfly/extension/grpc"), true);
        grpcSubsystem.addImportFilter(PathFilters.isOrIsChildOf("META-INF/services"), true);
        grpcSubsystem.addImportFilter(PathFilters.acceptAll(), false);
        moduleSpecification.addSystemDependency(grpcSubsystem);
    }

    private ModuleDependency cdiDependency(ModuleDependency moduleDependency) {
        // This is needed following https://issues.redhat.com/browse/WFLY-13641 /
        // https://github.com/wildfly/wildfly/pull/13406
        moduleDependency.addImportFilter(s -> s.equals("META-INF"), true);
        return moduleDependency;
    }
}

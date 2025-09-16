/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.grpc;

import java.util.List;

import org.jboss.as.server.deployment.DeploymentUnit;

import io.grpc.BindableService;
import io.grpc.ServerInterceptor;

/**
 * A registry for registering {@linkplain BindableService services} with a deployment.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public interface WildFlyGrpcDeploymentRegistry {

    /**
     * Adds a {@link BindableService} to the gRPC server.
     *
     * @param serviceType
     *                         the service to add
     * @param interceptors
     *                         {@link ServerInterceptor}s to wrap around the service
     */
    void addService(DeploymentUnit deployment, Class<? extends BindableService> serviceType,
            List<ServerInterceptor> interceptors);

    /**
     * Removes all the associated services from the gRPC server.
     *
     * @param deployment
     *                       the name of the deployment to remove the services for
     */
    void removeDeploymentServices(DeploymentUnit deployment);
}

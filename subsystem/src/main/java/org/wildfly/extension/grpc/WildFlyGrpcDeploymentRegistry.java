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
     * @param serviceType  the service to add
     * @param interceptors {@link ServerInterceptor}s to wrap around the service
     */
    void addService(DeploymentUnit deployment, Class<? extends BindableService> serviceType,
            List<ServerInterceptor> interceptors);

    /**
     * Removes all the associated services from the gRPC server.
     *
     * @param deployment the name of the deployment to remove the services for
     */
    void removeDeploymentServices(DeploymentUnit deployment);
}

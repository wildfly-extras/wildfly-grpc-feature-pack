/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.grpc;

import java.util.List;

import io.grpc.BindableService;
import io.grpc.ServerInterceptor;
import io.grpc.ServerServiceDefinition;

/**
 * A registry for registering {@linkplain BindableService services} with a deployment.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public interface WildFlyGrpcDeploymentRegistry {

    /**
     * Adds a {@link BindableService} instance to the gRPC server.
     *
     * @param service
     *                         the service instance to add
     * @param interceptors
     *                         {@link ServerInterceptor}s to wrap around the service
     * @return the registered {@link ServerServiceDefinition}
     */
    ServerServiceDefinition addService(BindableService service, List<ServerInterceptor> interceptors);

    /**
     * Removes a previously registered service from the gRPC server.
     *
     * @param ssd
     *                the service definition to remove
     */
    void removeService(ServerServiceDefinition ssd);
}

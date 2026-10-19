/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.grpc;

import java.util.List;

import org.jboss.msc.service.StartException;

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
     * @param deploymentName
     *                           the name of the owning deployment
     * @param service
     *                           the service instance to add
     * @param interceptors
     *                           {@link ServerInterceptor}s to wrap around the service
     * @return the registered {@link ServerServiceDefinition}
     * @throws StartException
     *                            if the gRPC service name is already registered by another deployment
     */
    ServerServiceDefinition addService(String deploymentName, BindableService service, List<ServerInterceptor> interceptors)
            throws StartException;

    /**
     * Removes a previously registered service from the gRPC server.
     *
     * @param ssd
     *                the service definition to remove
     */
    void removeService(ServerServiceDefinition ssd);

    /**
     * Returns {@code true} if the backing Undertow server has at least one HTTP/2-capable listener.
     * An HTTPS listener always supports h2 via ALPN. A plain HTTP listener requires
     * {@code enable-http2="true"} to support h2c, but that setting cannot be detected
     * via the public API — only HTTPS listeners can be reliably identified here.
     * <p>
     * Returns {@code true} by default (no warning) when the server has not yet started.
     */
    default boolean isHttp2Configured() {
        return true;
    }
}

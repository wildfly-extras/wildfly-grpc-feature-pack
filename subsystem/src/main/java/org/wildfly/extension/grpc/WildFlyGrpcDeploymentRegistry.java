/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.grpc;

import org.jboss.msc.service.StartException;

/**
 * Routes gRPC service names to the WAR context paths that host them.
 */
public interface WildFlyGrpcDeploymentRegistry {

    /**
     * Registers a gRPC service name and the WAR context path that serves it.
     * The gRPC subsystem filter uses this mapping to route incoming requests.
     *
     * @param serviceName
     *                        the fully-qualified gRPC service name (e.g. {@code helloworld.Greeter})
     * @param contextPath
     *                        the WAR context path (e.g. {@code /my-app})
     * @throws StartException
     *                            if the service name is already registered by another deployment
     */
    void registerService(String serviceName, String contextPath) throws StartException;

    /**
     * Removes a previously registered gRPC service name from the routing table.
     *
     * @param serviceName
     *                        the fully-qualified gRPC service name to remove
     */
    void unregisterService(String serviceName);

    /**
     * Returns {@code true} if the backing Undertow server has at least one HTTP/2-capable listener.
     * An HTTPS listener always supports h2 via ALPN. A plain HTTP listener requires
     * {@code enable-http2="true"} to support h2c.
     * <p>
     * Returns {@code true} by default (no warning) when the server has not yet started.
     */
    default boolean isHttp2Configured() {
        return true;
    }
}

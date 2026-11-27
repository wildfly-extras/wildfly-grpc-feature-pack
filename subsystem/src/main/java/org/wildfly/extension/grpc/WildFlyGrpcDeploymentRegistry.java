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

}

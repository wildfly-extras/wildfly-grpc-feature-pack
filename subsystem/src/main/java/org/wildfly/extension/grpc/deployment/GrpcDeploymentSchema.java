/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.grpc.deployment;

public enum GrpcDeploymentSchema {

    GRPC_1_0("urn:wildfly:grpc:1.0"),;

    private final String name;

    GrpcDeploymentSchema(String name) {
        this.name = name;
    }

    /**
     * Get the URI of this namespace.
     *
     * @return the URI
     */
    public String getUriString() {
        return name;
    }
}

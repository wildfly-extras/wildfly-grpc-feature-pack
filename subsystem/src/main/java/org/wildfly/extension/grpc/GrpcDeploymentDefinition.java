/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.grpc;

import org.jboss.as.controller.SimpleResourceDefinition;

import static org.wildfly.extension.grpc.GrpcSubsystemRegistrar.RESOLVER;

public class GrpcDeploymentDefinition extends SimpleResourceDefinition {

    static final GrpcDeploymentDefinition INSTANCE = new GrpcDeploymentDefinition();

    public GrpcDeploymentDefinition() {
        super(Paths.SUBSYSTEM, RESOLVER);
    }
}

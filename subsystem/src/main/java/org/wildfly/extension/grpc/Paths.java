/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.grpc;

import org.jboss.as.controller.PathElement;

import static org.wildfly.extension.grpc.GrpcExtension.SUBSYSTEM_NAME;

public interface Paths {

    PathElement GRPC_SERVICE = PathElement.pathElement(Constants.GRPC_SERVICE);
    PathElement SUBSYSTEM = PathElement.pathElement(Constants.SUBSYSTEM, SUBSYSTEM_NAME);
}

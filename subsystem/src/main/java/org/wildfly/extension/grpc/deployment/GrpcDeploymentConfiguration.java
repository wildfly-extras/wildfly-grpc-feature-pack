/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.grpc.deployment;

public interface GrpcDeploymentConfiguration {

    String getHost();

    int getPort();

    String getKeyManager();
}

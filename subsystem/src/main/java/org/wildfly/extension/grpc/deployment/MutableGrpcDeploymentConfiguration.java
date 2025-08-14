/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.grpc.deployment;

public class MutableGrpcDeploymentConfiguration implements GrpcDeploymentConfiguration {

    private String host;
    private int port;
    private String keyManager;

    void setHost(final String host) {
        this.host = host;
    }

    @Override
    public String getHost() {
        return host;
    }

    void setPort(final int port) {
        this.port = port;
    }

    @Override
    public int getPort() {
        return port;
    }

    void setKeyManager(final String keyManager) {
        this.keyManager = keyManager;
    }

    @Override
    public String getKeyManager() {
        return keyManager;
    }
}

/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.grpc;

public interface Capabilities {

    /*
     * Security Capabilities
     */
    String KEY_MANAGER_CAPABILITY = "org.wildfly.security.key-manager";
    String SSL_CONTEXT_CAPABILITY = "org.wildfly.security.ssl-context";
    String TRUST_MANAGER_CAPABILITY = "org.wildfly.security.trust-manager";
    /*
     * Socket Capabilities
     */
    String SOCKET_BiNDING = "org.wildfly.network.socket-binding";
}

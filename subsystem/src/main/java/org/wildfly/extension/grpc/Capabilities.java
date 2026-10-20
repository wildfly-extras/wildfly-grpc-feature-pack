/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.grpc;

public interface Capabilities {

    String HTTP_AUTHENTICATION_FACTORY_CAPABILITY = "org.wildfly.security.http-authentication-factory";
    String UNDERTOW_SERVER_CAPABILITY = "org.wildfly.undertow.server";
    String UNDERTOW_HOST_CAPABILITY = "org.wildfly.undertow.host";
    String WELD_CAPABILITY = "org.wildfly.weld";
}

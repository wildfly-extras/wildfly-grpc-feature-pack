/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.feature.pack.grpc.test.cdi;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PrefixService {

    public String value() {
        return "[CDI]";
    }
}

/*
 *  Copyright 2022 Red Hat
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.wildfly.extension.grpc;

import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.AttributeDefinition;

public class GrpcConstants {

    public static final String GRPC_SERVER_HOST = "wildfly-grpc-server-host";
    public static final String GRPC_SERVER_PORT = "wildfly-grpc-server-port";

    public static final Map<String, AttributeDefinition> nameToAttributeMap = new HashMap<String, AttributeDefinition>();
    static {
        nameToAttributeMap.put(GRPC_SERVER_HOST, GrpcAttribute.GRPC_SERVER_HOST);
        nameToAttributeMap.put(GRPC_SERVER_PORT, GrpcAttribute.GRPC_SERVER_PORT);
    }
}

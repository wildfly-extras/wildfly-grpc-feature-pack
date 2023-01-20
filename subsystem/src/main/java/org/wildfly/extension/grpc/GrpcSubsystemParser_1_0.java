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

import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentResourceXMLParser;

import static org.jboss.as.controller.PersistentResourceXMLDescription.builder;

/**
 */
public class GrpcSubsystemParser_1_0 extends PersistentResourceXMLParser {
    public static final String NAMESPACE = "urn:wildfly:grpc:1.0";

    private static final PersistentResourceXMLDescription xmlDescription;

    static {
        xmlDescription = builder(GrpcSubsystemDefinition.INSTANCE.getPathElement(), GrpcSubsystemSchema.GRPC_1_0.getUriString())
                .addAttributes(
                        GrpcSubsystemDefinition.GRPC_FLOW_CONTROL_WINDOW,
                        GrpcSubsystemDefinition.GRPC_HANDSHAKE_TIMEOUT,
                        GrpcSubsystemDefinition.GRPC_INITIAL_FLOW_CONTROL_WINDOW,
                        GrpcSubsystemDefinition.GRPC_KEEP_ALIVE_TIME,
                        GrpcSubsystemDefinition.GRPC_KEEP_ALIVE_TIMEOUT,
                        GrpcSubsystemDefinition.GRPC_KEY_MANAGER_NAME,
                        GrpcSubsystemDefinition.GRPC_MAX_CONCURRENT_CALLS_PER_CONNECTION,
                        GrpcSubsystemDefinition.GRPC_MAX_CONNECTION_AGE,
                        GrpcSubsystemDefinition.GRPC_MAX_CONNECTION_AGE_GRACE,
                        GrpcSubsystemDefinition.GRPC_MAX_CONNECTION_IDLE,
                        GrpcSubsystemDefinition.GRPC_MAX_INBOUND_MESSAGE_SIZE,
                        GrpcSubsystemDefinition.GRPC_MAX_INBOUND_METADATA_SIZE,
                        GrpcSubsystemDefinition.GRPC_PERMIT_KEEP_ALIVE_TIME,
                        GrpcSubsystemDefinition.GRPC_PERMIT_KEEP_ALIVE_WITHOUT_CALLS,
                        GrpcSubsystemDefinition.GRPC_PROTOCOL_PROVIDER,
                        GrpcSubsystemDefinition.GRPC_SERVER_HOST,
                        GrpcSubsystemDefinition.GRPC_SERVER_PORT,
                        GrpcSubsystemDefinition.GRPC_SESSION_CACHE_SIZE,
                        GrpcSubsystemDefinition.GRPC_SESSION_TIMEOUT,
                        GrpcSubsystemDefinition.GRPC_SHUTDOWN_TIMEOUT,
                        GrpcSubsystemDefinition.GRPC_SSL_CONTEXT_NAME,
                        GrpcSubsystemDefinition.GRPC_START_TLS,
                        GrpcSubsystemDefinition.GRPC_TRUST_MANAGER_NAME)
                .build();
    }

    @Override
    public PersistentResourceXMLDescription getParserDescription() {
        return xmlDescription;
    }
}

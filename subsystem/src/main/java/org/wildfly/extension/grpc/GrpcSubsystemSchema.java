/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.grpc;

import static org.jboss.as.controller.PersistentResourceXMLDescription.builder;
import static org.wildfly.extension.grpc.GrpcExtension.SUBSYSTEM_NAME;
import static org.wildfly.extension.grpc.GrpcExtension.SUBSYSTEM_PATH;

import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentSubsystemSchema;
import org.jboss.as.controller.SubsystemSchema;
import org.jboss.as.controller.xml.VersionedNamespace;
import org.jboss.as.version.Stability;
import org.jboss.staxmapper.IntVersion;

enum GrpcSubsystemSchema implements PersistentSubsystemSchema<GrpcSubsystemSchema> {
    VERSION_1_0_PREVIEW(1, 0, Stability.PREVIEW),;

    static final GrpcSubsystemSchema CURRENT = VERSION_1_0_PREVIEW;

    private final VersionedNamespace<IntVersion, GrpcSubsystemSchema> namespace;

    GrpcSubsystemSchema(int major, int minor, Stability stability) {
        this.namespace = SubsystemSchema.createSubsystemURN(SUBSYSTEM_NAME, stability, new IntVersion(major, minor));
    }

    @Override
    public VersionedNamespace<IntVersion, GrpcSubsystemSchema> getNamespace() {
        return namespace;
    }

    @Override
    public PersistentResourceXMLDescription getXMLDescription() {
        // TODO - How can this avoid this deprecated variant?
        return builder(SUBSYSTEM_PATH, namespace).addAttributes(GrpcSubsystemDefinition.GRPC_FLOW_CONTROL_WINDOW,
                GrpcSubsystemDefinition.GRPC_HANDSHAKE_TIMEOUT,
                GrpcSubsystemDefinition.GRPC_INITIAL_FLOW_CONTROL_WINDOW, GrpcSubsystemDefinition.GRPC_KEEP_ALIVE_TIME,
                GrpcSubsystemDefinition.GRPC_KEEP_ALIVE_TIMEOUT, GrpcSubsystemDefinition.GRPC_KEY_MANAGER_NAME,
                GrpcSubsystemDefinition.GRPC_MAX_CONCURRENT_CALLS_PER_CONNECTION,
                GrpcSubsystemDefinition.GRPC_MAX_CONNECTION_AGE, GrpcSubsystemDefinition.GRPC_MAX_CONNECTION_AGE_GRACE,
                GrpcSubsystemDefinition.GRPC_MAX_CONNECTION_IDLE, GrpcSubsystemDefinition.GRPC_MAX_INBOUND_MESSAGE_SIZE,
                GrpcSubsystemDefinition.GRPC_MAX_INBOUND_METADATA_SIZE,
                GrpcSubsystemDefinition.GRPC_PERMIT_KEEP_ALIVE_TIME,
                GrpcSubsystemDefinition.GRPC_PERMIT_KEEP_ALIVE_WITHOUT_CALLS,
                GrpcSubsystemDefinition.GRPC_PROTOCOL_PROVIDER, GrpcSubsystemDefinition.GRPC_SERVER_SOCKET_BINDING,
                GrpcSubsystemDefinition.GRPC_SESSION_CACHE_SIZE, GrpcSubsystemDefinition.GRPC_SESSION_TIMEOUT,
                GrpcSubsystemDefinition.GRPC_SHUTDOWN_TIMEOUT, GrpcSubsystemDefinition.GRPC_SSL_CONTEXT_NAME,
                GrpcSubsystemDefinition.GRPC_START_TLS, GrpcSubsystemDefinition.GRPC_TRUST_MANAGER_NAME).build();
    }

}

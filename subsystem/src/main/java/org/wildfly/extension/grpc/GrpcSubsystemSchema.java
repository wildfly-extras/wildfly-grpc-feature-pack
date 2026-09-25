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
        return builder(SUBSYSTEM_PATH, namespace)
                .addAttributes(GrpcSubsystemDefinition.GRPC_MAX_INBOUND_MESSAGE_SIZE,
                        GrpcSubsystemDefinition.GRPC_MAX_INBOUND_METADATA_SIZE,
                        GrpcSubsystemDefinition.GRPC_SERVER_NAME,
                        GrpcSubsystemDefinition.GRPC_SHUTDOWN_TIMEOUT,
                        GrpcSubsystemDefinition.GRPC_VIRTUAL_HOST)
                .build();
    }
}

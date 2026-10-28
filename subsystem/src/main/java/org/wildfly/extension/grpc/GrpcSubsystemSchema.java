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
    // 1.0: Netty-based server (removed in servlet rewrite — kept so the parser rejects old
    // configs with an "unsupported namespace" error rather than silently misparsing them)
    VERSION_1_0_PREVIEW(1, 0, Stability.PREVIEW) {
        @Override
        public PersistentResourceXMLDescription getXMLDescription() {
            // Netty-based schema: no attributes are valid in the new model.
            // Returning an empty description causes the parser to reject unknown attributes,
            // surfacing a clear error rather than silently dropping Netty-only configuration.
            // TODO - How can this avoid this deprecated variant?
            return builder(SUBSYSTEM_PATH, getNamespace()).build();
        }
    },
    // 2.0: Undertow/servlet-based server
    VERSION_2_0_PREVIEW(2, 0, Stability.PREVIEW) {
        @Override
        public PersistentResourceXMLDescription getXMLDescription() {
            // TODO - How can this avoid this deprecated variant?
            return builder(SUBSYSTEM_PATH, getNamespace())
                    .addAttributes(GrpcSubsystemDefinition.MAX_INBOUND_MESSAGE_SIZE,
                            GrpcSubsystemDefinition.MAX_INBOUND_METADATA_SIZE,
                            GrpcSubsystemDefinition.SERVER_NAME,
                            GrpcSubsystemDefinition.VIRTUAL_HOST)
                    .build();
        }
    },;

    static final GrpcSubsystemSchema CURRENT = VERSION_2_0_PREVIEW;

    private final VersionedNamespace<IntVersion, GrpcSubsystemSchema> namespace;

    GrpcSubsystemSchema(int major, int minor, Stability stability) {
        this.namespace = SubsystemSchema.createSubsystemURN(SUBSYSTEM_NAME, stability, new IntVersion(major, minor));
    }

    @Override
    public VersionedNamespace<IntVersion, GrpcSubsystemSchema> getNamespace() {
        return namespace;
    }
}

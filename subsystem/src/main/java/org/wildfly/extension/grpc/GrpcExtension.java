/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.grpc;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemModel;
import org.jboss.as.version.Stability;
import org.wildfly.subsystem.SubsystemConfiguration;
import org.wildfly.subsystem.SubsystemExtension;
import org.wildfly.subsystem.SubsystemPersistence;

public class GrpcExtension extends SubsystemExtension<GrpcSubsystemSchema> {

    public static final String SUBSYSTEM_NAME = "grpc";
    // TODO - Can we get rid of this path?
    static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME);

    private static final Stability FEATURE_STABILITY = Stability.PREVIEW;

    public GrpcExtension() {
        super(SubsystemConfiguration.of(SUBSYSTEM_NAME, GrpcSubsystemModel.CURRENT, GrpcSubsystemRegistrar::new),
                SubsystemPersistence.of(GrpcSubsystemSchema.CURRENT));
    }

    @Override
    public Stability getStability() {
        return FEATURE_STABILITY;
    }

    /**
     * Model for the grpc subsystem.
     */
    enum GrpcSubsystemModel implements SubsystemModel {
        VERSION_1_0_0(1, 0, 0),;

        static final GrpcSubsystemModel CURRENT = VERSION_1_0_0;

        private final ModelVersion version;

        GrpcSubsystemModel(int major, int minor, int micro) {
            this.version = ModelVersion.create(major, minor, micro);
        }

        @Override
        public ModelVersion getVersion() {
            return version;
        }
    }

}

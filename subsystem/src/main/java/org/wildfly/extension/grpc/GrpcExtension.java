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

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemModel;
import org.jboss.as.version.Stability;
import org.wildfly.subsystem.SubsystemConfiguration;
import org.wildfly.subsystem.SubsystemExtension;
import org.wildfly.subsystem.SubsystemPersistence;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

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
        VERSION_1_0_0(1, 0, 0),
        ;

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

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

import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.grpc._private.GrpcLogger;

public class GrpcSubsystemService implements Service {

    public static final ServiceName SERVICE_NAME = ServiceName.of("io.grpc.server");

    GrpcSubsystemService() {
    }

    @Override
    public void start(StartContext context) {
        GrpcLogger.LOGGER.grpcStarting();
    }

    @Override
    public void stop(StopContext context) {
        GrpcLogger.LOGGER.grpcStopping();
    }
}

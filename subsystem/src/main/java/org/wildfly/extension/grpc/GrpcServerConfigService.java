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

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 */
@SuppressWarnings("deprecation")
public final class GrpcServerConfigService implements Service<GrpcServerConfig> {

    public static final ServiceName GRPC_SERVICE = ServiceName.JBOSS.append("grpc");
    public static final ServiceName CONFIG_SERVICE = GRPC_SERVICE.append("config");

    private final GrpcServerConfig serverConfig;

    private GrpcServerConfigService(final GrpcServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    @Override
    public void start(final StartContext context) throws StartException {
    }

    @Override
    public void stop(final StopContext context) {
    }

    public static ServiceController<?> install(final ServiceTarget serviceTarget, final GrpcServerConfig serverConfig) {
        final ServiceBuilder<?> builder = serviceTarget.addService(CONFIG_SERVICE);
        builder.setInstance(new GrpcServerConfigService(serverConfig));
        return builder.install();
    }

    public GrpcServerConfig getGrpcServerConfig() {
        return serverConfig;
    }

    @Override
    public GrpcServerConfig getValue() throws IllegalStateException, IllegalArgumentException {
        return serverConfig;
    }
}

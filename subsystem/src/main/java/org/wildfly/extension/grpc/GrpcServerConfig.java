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

import org.jboss.dmr.ModelNode;

/**
 * @author <a href="mailto:rsigal@redhat.com">Ron Sigal</a>
 */
public class GrpcServerConfig {

    private ModelNode wildflyGrpcServerPort;
    private ModelNode wildflyGrpcServerHost;

    public ModelNode getWildflyGrpcServerPort() {
        return wildflyGrpcServerPort;
    }

    public void setWildflyGrpcServerPort(ModelNode wildflyGrpcServerPort) {
        this.wildflyGrpcServerPort = wildflyGrpcServerPort;
    }

    public ModelNode getWildflyGrpcServerHost() {
        return wildflyGrpcServerHost;
    }

    public void setWildflyGrpcServerHost(ModelNode wildflyGrpcServerHost) {
        this.wildflyGrpcServerHost = wildflyGrpcServerHost;
    }
}

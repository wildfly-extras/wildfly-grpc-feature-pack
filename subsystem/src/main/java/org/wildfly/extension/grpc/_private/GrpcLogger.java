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
package org.wildfly.extension.grpc._private;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

import static org.jboss.logging.Logger.Level.*;

@MessageLogger(projectCode = "WFLYGRPC", length = 4)
public interface GrpcLogger extends BasicLogger {

    GrpcLogger LOGGER = Logger.getMessageLogger(GrpcLogger.class, "org.wildfly.extension.grpc");

    @LogMessage(level = INFO)
    @Message(id = 1, value = "gRPC service starting")
    void grpcStarting();

    @LogMessage(level = INFO)
    @Message(id = 2, value = "gRPC service stopping")
    void grpcStopping();

    @LogMessage(level = INFO)
    @Message(id = 3, value = "gRPC server for %s listening on %s:%d")
    void serverListening(String name, String address, int port);

    @LogMessage(level = INFO)
    @Message(id = 4, value = "gRPC server for %s stopping")
    void serverStopping(String name);

    @LogMessage(level = INFO)
    @Message(id = 5, value = "gRPC service %s registered")
    void registerService(String name);

    @LogMessage(level = ERROR)
    @Message(id = 6, value = "unknown Attribute: %s")
    void unknownAttribute(String attribute);
}

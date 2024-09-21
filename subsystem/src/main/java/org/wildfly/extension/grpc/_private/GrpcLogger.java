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
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;

@MessageLogger(projectCode = "WFLYGRPC", length = 4)
public interface GrpcLogger extends BasicLogger {

    GrpcLogger LOGGER = Logger.getMessageLogger(GrpcLogger.class, "org.wildfly.extension.grpc");

    @LogMessage(level = INFO)
    @Message(id = 1, value = "gRPC server listening on %s:%d")
    void serverListening(String address, int port);

    @LogMessage(level = INFO)
    @Message(id = 2, value = "gRPC service stopping")
    void grpcStopping();

    @LogMessage(level = ERROR)
    @Message(id = 3, value = "Failed to stop gRPC server")
    void failedToStopGrpcServer(@Cause Throwable cause);

    @Message(id = 4, value = "Failed to register %s")
    RuntimeException failedToRegister(@Cause Throwable cause, String serviceName);

    @Message(id = 5, value = "Failed to register %s for deployment %s")
    RuntimeException failedToRegister(@Cause Throwable cause, String serviceName, String deployment);

    @LogMessage(level = DEBUG)
    @Message(id = 6, value = "Registering gRPC service %s for deployment %s.")
    void registerService(String serviceName, String deploymentName);

    @LogMessage(level = DEBUG)
    @Message(id = 7, value = "Registering global gRPC ServerInterceptor %s.")
    void registerServerInterceptor(String interceptorName);

    @LogMessage(level = DEBUG)
    @Message(id = 8, value = "Registering gRPC ServerInterceptor %s for deployment %s.")
    void registerServerInterceptor(String interceptorName, String deploymentName);

    @LogMessage(level = ERROR)
    @Message(id = 9, value = "Method %s is not implemented.")
    void methodNotImplemented(String methodName);

}

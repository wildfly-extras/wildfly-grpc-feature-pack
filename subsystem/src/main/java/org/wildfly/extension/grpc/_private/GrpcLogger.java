/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.grpc._private;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

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

    @Message(id = 10, value = "The configuration object has already been built.")
    IllegalStateException configurationAlreadyBuilt();

}

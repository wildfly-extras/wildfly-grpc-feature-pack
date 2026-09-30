/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.grpc._private;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

import java.lang.invoke.MethodHandles;

import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.msc.service.StartException;

@MessageLogger(projectCode = "WFLYGRPC", length = 4)
public interface GrpcLogger extends BasicLogger {

    GrpcLogger LOGGER = Logger.getMessageLogger(MethodHandles.lookup(), GrpcLogger.class, "org.wildfly.extension.grpc");

    @LogMessage(level = INFO)
    @Message(id = 1, value = "gRPC server listening on %s:%d")
    void serverListening(String address, int port);

    @LogMessage(level = INFO)
    @Message(id = 2, value = "gRPC service stopping")
    void grpcStopping();

    @LogMessage(level = ERROR)
    @Message(id = 3, value = "Failed to stop gRPC server")
    void failedToStopGrpcServer(@Cause Throwable cause);

    @Message(id = 5, value = "Failed to register %s for deployment %s")
    StartException failedToRegister(@Cause Throwable cause, String serviceName, String deployment);

    @LogMessage(level = ERROR)
    @Message(id = 9, value = "Method %s is not implemented.")
    void methodNotImplemented(String methodName);

    @Message(id = 10, value = "The configuration object has already been built.")
    IllegalStateException configurationAlreadyBuilt();

    @Message(id = 11, value = "gRPC service '%s' is already registered by deployment '%s'")
    DeploymentUnitProcessingException grpcServiceAlreadyRegistered(String serviceName, String existingOwner);

    @LogMessage(level = WARN)
    @Message(id = 12, value = "Weld capability not available for deployment %s; CDI injection will not be available for gRPC services.")
    void weldCapabilityUnavailable(@Cause Throwable cause, String deploymentName);
}

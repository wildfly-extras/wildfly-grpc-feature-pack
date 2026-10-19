/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.grpc._private;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

import java.lang.invoke.MethodHandles;

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
    @Message(id = 1, value = "gRPC server registered on Undertow virtual host '%s'")
    void grpcServingViaUndertow(String hostName);

    @LogMessage(level = INFO)
    @Message(id = 2, value = "gRPC service stopping")
    void grpcStopping();

    @LogMessage(level = ERROR)
    @Message(id = 3, value = "Failed to stop gRPC server")
    void failedToStopGrpcServer(@Cause Throwable cause);

    @Message(id = 4, value = "Failed to register %s")
    RuntimeException failedToRegister(@Cause Throwable cause, String serviceName);

    @Message(id = 5, value = "Failed to register %s for deployment %s")
    StartException failedToRegister(@Cause Throwable cause, String serviceName, String deployment);

    @LogMessage(level = ERROR)
    @Message(id = 9, value = "Method %s is not implemented.")
    void methodNotImplemented(String methodName);

    @LogMessage(level = ERROR)
    @Message(id = 10, value = "Failed to undeploy gRPC server")
    void failedToUndeployGrpcServer(@Cause Throwable cause);

    @Message(id = 11, value = "gRPC service '%s' is already registered by deployment '%s'")
    StartException duplicateGrpcServiceName(String serviceName, String existingOwner);

    @LogMessage(level = WARN)
    @Message(id = 12, value = "Weld capability not available for deployment %s; CDI injection will not be available for gRPC services.")
    void weldCapabilityUnavailable(@Cause Throwable cause, String deploymentName);

    @LogMessage(level = WARN)
    @Message(id = 13, value = "Exception during gRPC service cleanup; resources may not have been fully released.")
    void failedToCleanupGrpcResources(@Cause Throwable cause);

    @LogMessage(level = WARN)
    @Message(id = 14, value = "Deployment '%s' contains gRPC services but no HTTP/2-capable listener is configured "
            + "on the Undertow server. gRPC requires HTTP/2 (h2 or h2c). Configure an https-listener (recommended "
            + "for production), or set enable-http2=\"true\" on the http-listener for plaintext h2c. "
            + "gRPC calls will fail until HTTP/2 is available.")
    void noHttp2ListenerConfigured(String deploymentName);
}

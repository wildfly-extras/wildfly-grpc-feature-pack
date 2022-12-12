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

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

/**
 * An AbstractWriteAttributeHandler extension for updating basic gRPC config attributes
 */
final class GrpcParamHandler extends AbstractWriteAttributeHandler<Void> {

    public GrpcParamHandler(final AttributeDefinition... definitions) {
        super(definitions);
    }

    @Override
    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
            ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<Void> handbackHolder)
            throws OperationFailedException {

        // If the required value is the current one, we do not need to do anything.
        if (!isSameValue(context, resolvedValue, currentValue, attributeName)) {
            updateServerConfig(context, attributeName, resolvedValue, false);
        }
        return false;
    }

    private boolean isSameValue(OperationContext context, ModelNode resolvedValue, ModelNode currentValue, String attributeName)
            throws OperationFailedException {
        if (resolvedValue.equals(getAttributeDefinition(attributeName).resolveValue(context, currentValue))) {
            return true;
        }
        if (!currentValue.isDefined()) {
            return resolvedValue.equals(getAttributeDefinition(attributeName).getDefaultValue());
        }
        return false;
    }

    @Override
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
            ModelNode valueToRestore, ModelNode valueToRevert, Void handback) throws OperationFailedException {
        updateServerConfig(null, attributeName, valueToRestore, true);
    }

    /**
     * Returns true if the update operation succeeds in modifying the runtime, false otherwise.
     *
     * @param context TODO
     * @param attributeName
     * @param value
     */
    private void updateServerConfig(OperationContext context, String attributeName, ModelNode value, boolean isRevert)
            throws OperationFailedException {
        ServiceRegistry registry = context.getServiceRegistry(true);
        ServiceName name = GrpcServerConfigService.CONFIG_SERVICE;

        @SuppressWarnings("deprecation") GrpcServerConfig config = (GrpcServerConfig) registry.getRequiredService(name)
                .getValue();

        switch (attributeName) {

            case GrpcConstants.GRPC_SERVER_HOST:
                config.setWildflyGrpcServerHost(value);
                break;

            case GrpcConstants.GRPC_SERVER_PORT:
                config.setWildflyGrpcServerPort(value);
                break;

            default:
                throw new RuntimeException("unrecognized attribute: " + attributeName);
        }
    }
}

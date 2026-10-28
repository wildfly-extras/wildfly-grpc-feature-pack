/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.grpc.deployment;

import java.util.List;

import org.jboss.as.server.deployment.AttachmentKey;

import io.grpc.BindableService;
import io.grpc.util.MutableHandlerRegistry;

public final class GrpcDeploymentAttachments {

    @SuppressWarnings("unchecked")
    public static final AttachmentKey<List<Class<? extends BindableService>>> GRPC_BINDABLE_SERVICES = (AttachmentKey<List<Class<? extends BindableService>>>) (AttachmentKey<?>) AttachmentKey
            .create(List.class);

    @SuppressWarnings("unchecked")
    public static final AttachmentKey<List<String>> INTERCEPTOR_CLASSES = (AttachmentKey<List<String>>) (AttachmentKey<?>) AttachmentKey
            .create(List.class);

    public static final AttachmentKey<MutableHandlerRegistry> GRPC_HANDLER_REGISTRY = AttachmentKey
            .create(MutableHandlerRegistry.class);

    private GrpcDeploymentAttachments() {
    }
}

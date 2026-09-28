/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.grpc;

/**
 * Configuration for the {@link GrpcUndertowService}.
 */
record ServletConfiguration(int maxInboundMessageSize, int maxInboundMetadataSize) {
}

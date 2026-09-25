/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.grpc;

import static org.wildfly.extension.grpc._private.GrpcLogger.LOGGER;

/**
 * A simple configuration for the {@link GrpcUndertowService}.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@SuppressWarnings("UnusedReturnValue")
class ServerConfiguration {

    private volatile boolean built = false;

    private long shutdownTimeout;
    private int maxInboundMessageSize;
    private int maxInboundMetadataSize;

    long getShutdownTimeout() {
        return shutdownTimeout;
    }

    ServerConfiguration setShutdownTimeout(final long shutdownTimeout) {
        assertNotBuilt();
        this.shutdownTimeout = shutdownTimeout;
        return this;
    }

    ServerConfiguration setMaxInboundMessageSize(final int maxInboundMessageSize) {
        assertNotBuilt();
        this.maxInboundMessageSize = maxInboundMessageSize;
        return this;
    }

    int getMaxInboundMessageSize() {
        return maxInboundMessageSize;
    }

    ServerConfiguration setMaxInboundMetadataSize(final int maxInboundMetadataSize) {
        assertNotBuilt();
        this.maxInboundMetadataSize = maxInboundMetadataSize;
        return this;
    }

    int getMaxInboundMetadataSize() {
        return maxInboundMetadataSize;
    }

    ServerConfiguration build() {
        built = true;
        return this;
    }

    private void assertNotBuilt() {
        if (built) {
            throw LOGGER.configurationAlreadyBuilt();
        }
    }
}

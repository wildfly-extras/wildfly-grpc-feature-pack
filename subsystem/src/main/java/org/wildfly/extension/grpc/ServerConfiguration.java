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
    private long keepLiveTime;
    private long keepAliveTimeout;
    private int maxConccurentCallsPerConnection;
    private long maxConnectionAge;
    private long maxConnectionAgeGrace;
    private long maxConnectionIdle;
    private int maxInboundMessageSize;
    private int maxInboundMetadataSize;
    private long permitKeepAliveTime;
    private boolean permitKeepAliveWithoutCalls;

    long getShutdownTimeout() {
        return shutdownTimeout;
    }

    ServerConfiguration setShutdownTimeout(final long shutdownTimeout) {
        assertNotBuilt();
        this.shutdownTimeout = shutdownTimeout;
        return this;
    }

    ServerConfiguration setKeepLiveTime(final long keepLiveTime) {
        assertNotBuilt();
        this.keepLiveTime = keepLiveTime;
        return this;
    }

    long getKeepLiveTime() {
        return keepLiveTime;
    }

    ServerConfiguration setKeepAliveTimeout(final long keepAliveTimeout) {
        assertNotBuilt();
        this.keepAliveTimeout = keepAliveTimeout;
        return this;
    }

    long getKeepAliveTimeout() {
        return keepAliveTimeout;
    }

    ServerConfiguration setMaxConcurrentCallsPerConnection(final int maxConccurentCallsPerConnection) {
        assertNotBuilt();
        this.maxConccurentCallsPerConnection = maxConccurentCallsPerConnection;
        return this;
    }

    int getMaxConcurrentCallsPerConnection() {
        return maxConccurentCallsPerConnection;
    }

    ServerConfiguration setMaxConnectionAge(final long maxConnectionAge) {
        assertNotBuilt();
        this.maxConnectionAge = maxConnectionAge;
        return this;
    }

    long getMaxConnectionAge() {
        return maxConnectionAge;
    }

    ServerConfiguration setMaxConnectionAgeGrace(final long maxConnectionAgeGrace) {
        assertNotBuilt();
        this.maxConnectionAgeGrace = maxConnectionAgeGrace;
        return this;
    }

    long getMaxConnectionAgeGrace() {
        return maxConnectionAgeGrace;
    }

    ServerConfiguration setMaxConnectionIdle(final long maxConnectionIdle) {
        assertNotBuilt();
        this.maxConnectionIdle = maxConnectionIdle;
        return this;
    }

    long getMaxConnectionIdle() {
        return maxConnectionIdle;
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

    ServerConfiguration setPermitKeepAliveTime(final long permitKeepAliveTime) {
        assertNotBuilt();
        this.permitKeepAliveTime = permitKeepAliveTime;
        return this;
    }

    long getPermitKeepAliveTime() {
        return permitKeepAliveTime;
    }

    ServerConfiguration setPermitKeepAliveWithoutCalls(final boolean permitKeepAliveWithoutCalls) {
        assertNotBuilt();
        this.permitKeepAliveWithoutCalls = permitKeepAliveWithoutCalls;
        return this;
    }

    boolean isPermitKeepAliveWithoutCalls() {
        return permitKeepAliveWithoutCalls;
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

/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.grpc;

import java.util.function.Supplier;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.jboss.as.network.SocketBinding;

import static org.wildfly.extension.grpc._private.GrpcLogger.LOGGER;

/**
 * A simple configuration for the {@link GrpcServerService}.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@SuppressWarnings("UnusedReturnValue")
class ServerConfiguration {

    private volatile boolean built = false;

    private Supplier<SocketBinding> socketBinding;
    private Supplier<TrustManager> trustManager;
    private Supplier<KeyManager> keyManager;
    private Supplier<SSLContext> sslContext;
    private long shutdownTimeout;
    private String protocolProvider;
    private Long sessionCacheSize;
    private Long sessionTimeout;
    private boolean startTls;
    private int flowControlWindow;
    private int handshakeTimeout;
    private int initialFlowControlWindow;
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

    ServerConfiguration(final Supplier<SocketBinding> socketBinding) {
        this.socketBinding = socketBinding;
    }

    int getServerPort() {
        return socketBinding.get().getAbsolutePort();
    }

    String getHostName() {
        return socketBinding.get().getAddress().getHostName();
    }

    Supplier<TrustManager> getTrustManager() {
        return trustManager;
    }

    ServerConfiguration setTrustManager(final Supplier<TrustManager> trustManager) {
        assertNotBuilt();
        this.trustManager = trustManager;
        return this;
    }

    Supplier<KeyManager> getKeyManager() {
        return keyManager;
    }

    ServerConfiguration setKeyManager(final Supplier<KeyManager> keyManager) {
        assertNotBuilt();
        this.keyManager = keyManager;
        return this;
    }

    Supplier<SSLContext> getSslContext() {
        return sslContext;
    }

    ServerConfiguration setSslContext(final Supplier<SSLContext> sslContext) {
        assertNotBuilt();
        this.sslContext = sslContext;
        return this;
    }

    long getShutdownTimeout() {
        return shutdownTimeout;
    }

    ServerConfiguration setShutdownTimeout(final long shutdownTimeout) {
        assertNotBuilt();
        this.shutdownTimeout = shutdownTimeout;
        return this;
    }

    String getProtocolProvider() {
        return protocolProvider;
    }

    ServerConfiguration setProtocolProvider(final String protocolProvider) {
        assertNotBuilt();
        this.protocolProvider = protocolProvider;
        return this;
    }

    Long getSessionCacheSize() {
        return sessionCacheSize;
    }

    ServerConfiguration setSessionCacheSize(final Long sessionCacheSize) {
        assertNotBuilt();
        this.sessionCacheSize = sessionCacheSize;
        return this;
    }

    Long getSessionTimeout() {
        return sessionTimeout;
    }

    ServerConfiguration setSessionTimeout(final Long sessionTimeout) {
        assertNotBuilt();
        this.sessionTimeout = sessionTimeout;
        return this;
    }

    boolean isStartTls() {
        return startTls;
    }

    ServerConfiguration setStartTls(final boolean startTls) {
        assertNotBuilt();
        this.startTls = startTls;
        return this;
    }

    ServerConfiguration setFlowControlWindow(final int flowControlWindow) {
        assertNotBuilt();
        this.flowControlWindow = flowControlWindow;
        return this;
    }

    int getFlowControlWindow() {
        return flowControlWindow;
    }

    ServerConfiguration setHandshakeTimeout(final int handshakeTimeout) {
        assertNotBuilt();
        this.handshakeTimeout = handshakeTimeout;
        return this;
    }

    int getHandshakeTimeout() {
        return handshakeTimeout;
    }

    ServerConfiguration setInitialFlowControlWindow(final int initialFlowControlWindow) {
        assertNotBuilt();
        this.initialFlowControlWindow = initialFlowControlWindow;
        return this;
    }

    int getInitialFlowControlWindow() {
        return initialFlowControlWindow;
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

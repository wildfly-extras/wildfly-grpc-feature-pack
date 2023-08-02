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

import java.util.function.Supplier;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

/**
 * A simple configuration for the {@link GrpcServerService}.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@SuppressWarnings("UnusedReturnValue")
class ServerConfiguration {

    private final String hostName;
    private Supplier<TrustManager> trustManager;
    private Supplier<KeyManager> keyManager;
    private Supplier<SSLContext> sslContext;
    private long shutdownTimeout;
    private String protocolProvider;
    private Long sessionCacheSize;
    private Long sessionTimeout;
    private boolean startTls;

    ServerConfiguration(final String hostName) {
        this.hostName = hostName;
    }

    String getHostName() {
        return hostName;
    }

    Supplier<TrustManager> getTrustManager() {
        return trustManager;
    }

    ServerConfiguration setTrustManager(final Supplier<TrustManager> trustManager) {
        this.trustManager = trustManager;
        return this;
    }

    Supplier<KeyManager> getKeyManager() {
        return keyManager;
    }

    ServerConfiguration setKeyManager(final Supplier<KeyManager> keyManager) {
        this.keyManager = keyManager;
        return this;
    }

    Supplier<SSLContext> getSslContext() {
        return sslContext;
    }

    ServerConfiguration setSslContext(final Supplier<SSLContext> sslContext) {
        this.sslContext = sslContext;
        return this;
    }

    long getShutdownTimeout() {
        return shutdownTimeout;
    }

    ServerConfiguration setShutdownTimeout(final long shutdownTimeout) {
        this.shutdownTimeout = shutdownTimeout;
        return this;
    }

    String getProtocolProvider() {
        return protocolProvider;
    }

    ServerConfiguration setProtocolProvider(final String protocolProvider) {
        this.protocolProvider = protocolProvider;
        return this;
    }

    Long getSessionCacheSize() {
        return sessionCacheSize;
    }

    ServerConfiguration setSessionCacheSize(final Long sessionCacheSize) {
        this.sessionCacheSize = sessionCacheSize;
        return this;
    }

    Long getSessionTimeout() {
        return sessionTimeout;
    }

    ServerConfiguration setSessionTimeout(final Long sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
        return this;
    }

    boolean isStartTls() {
        return startTls;
    }

    ServerConfiguration setStartTls(final boolean startTls) {
        this.startTls = startTls;
        return this;
    }
}

/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package com.example.grpc.chat;

import java.io.InputStream;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;

import io.grpc.ChannelCredentials;
import io.grpc.Grpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.TlsChannelCredentials;

public enum SslMode {

    NONE("none"),
    ONEWAY("oneway"),
    TWOWAY("twoway");

    private static final String HOST = "localhost";
    private static final int PORT = 9555;
    private static final String TARGET = HOST + ":" + PORT;

    private final String label;

    SslMode(String label) {
        this.label = label;
    }

    public static SslMode fromString(String value) {
        for (SslMode mode : values()) {
            if (mode.label.equals(value)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unrecognized ssl value: " + value
                + ". Use none, oneway, or twoway.");
    }

    public ManagedChannel createChannel() {
        try {
            ClassLoader classLoader = SslMode.class.getClassLoader();
            switch (this) {
                case NONE:
                    return ManagedChannelBuilder.forTarget(TARGET)
                            .usePlaintext().build();
                case ONEWAY:
                    try (InputStream trustStore = classLoader.getResourceAsStream("client.truststore.pem")) {
                        ChannelCredentials creds = TlsChannelCredentials.newBuilder().trustManager(trustStore).build();
                        return Grpc.newChannelBuilderForAddress(HOST, PORT, creds).build();
                    }
                case TWOWAY:
                    KeyStore clientKeyStore = KeyStore.getInstance("PKCS12");
                    try (InputStream clientKeyStoreStream = classLoader.getResourceAsStream("client.keystore.p12")) {
                        clientKeyStore.load(clientKeyStoreStream, "secret".toCharArray());
                    }
                    KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                    kmf.init(clientKeyStore, "secret".toCharArray());
                    try (InputStream trustStore = classLoader.getResourceAsStream("client.truststore.pem")) {
                        ChannelCredentials creds = TlsChannelCredentials.newBuilder().trustManager(trustStore)
                                .keyManager(kmf.getKeyManagers())
                                .build();
                        return Grpc.newChannelBuilderForAddress(HOST, PORT, creds).build();
                    }
                default:
                    throw new IllegalStateException("Unexpected ssl mode: " + this);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create gRPC channel for ssl mode: " + this, e);
        }
    }

    @Override
    public String toString() {
        return label;
    }
}

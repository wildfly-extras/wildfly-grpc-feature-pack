/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.feature.pack.grpc.test.utility;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.Executor;

import io.grpc.CallCredentials;
import io.grpc.Metadata;

public final class BasicCallCredentials extends CallCredentials {

    private final String encoded;

    public BasicCallCredentials(String username, String password) {
        this.encoded = Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void applyRequestMetadata(RequestInfo requestInfo, Executor appExecutor,
            MetadataApplier applier) {
        final Metadata metadata = new Metadata();
        metadata.put(
                Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER),
                "Basic " + encoded);
        applier.apply(metadata);
    }

    @Override
    public void thisUsesUnstableApi() {
    }
}

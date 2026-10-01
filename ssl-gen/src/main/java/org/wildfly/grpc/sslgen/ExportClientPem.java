/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.grpc.sslgen;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;

import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

/**
 * Exports the client certificate and private key from a PKCS12 keystore as PEM files for use with grpcurl.
 *
 * <p>Usage: ExportClientPem &lt;keystore&gt; &lt;password&gt; &lt;alias&gt; &lt;cert.pem&gt; &lt;key.pem&gt;
 */
public class ExportClientPem {

    public static void main(String[] args) throws Exception {
        String keystorePath = args[0];
        char[] password = args[1].toCharArray();
        String alias = args[2];
        String certPemPath = args[3];
        String keyPemPath = args[4];

        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(keystorePath)) {
            ks.load(fis, password);
        }

        Certificate cert = ks.getCertificate(alias);
        PrivateKey key = (PrivateKey) ks.getKey(alias, password);

        try (JcaPEMWriter writer = new JcaPEMWriter(new FileWriter(certPemPath))) {
            writer.writeObject(cert);
        }
        try (JcaPEMWriter writer = new JcaPEMWriter(new FileWriter(keyPemPath))) {
            writer.writeObject(key);
        }
    }
}

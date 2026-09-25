/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.feature.pack.grpc.test.utility;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.arquillian.setup.SnapshotServerSetupTask;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.dmr.ModelNode;

/**
 * Enables HTTP/2 cleartext (h2c) on the Undertow default HTTP listener so that
 * gRPC clients using {@code usePlaintext()} can connect on port 8080.
 */
public class EnableH2cServerSetupTask extends SnapshotServerSetupTask {

    @Override
    protected void doSetup(final ManagementClient client, final String containerId) throws Exception {
        ModelNode address = Operations.createAddress(
                "subsystem", "undertow", "server", "default-server", "http-listener", "default");
        ModelNode op = Operations.createWriteAttributeOperation(address, "enable-http2", new ModelNode(true));
        ModelNode result = client.getControllerClient().execute(op);
        if (!Operations.isSuccessfulOutcome(result)) {
            throw new RuntimeException("Failed to enable h2c: " + Operations.getFailureDescription(result));
        }
        ServerReload.reloadIfRequired(client.getControllerClient());
    }
}

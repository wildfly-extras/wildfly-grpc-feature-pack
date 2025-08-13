/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.grpc;

import java.io.IOException;

import org.jboss.as.controller.capability.registry.RuntimeCapabilityRegistry;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.AdditionalInitialization.ManagementAdditionalInitialization;
import org.jboss.as.version.Stability;
import org.junit.Test;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class SubsystemTestCase extends AbstractSubsystemBaseTest {

    public SubsystemTestCase() {
        super(GrpcExtension.SUBSYSTEM_NAME, new GrpcExtension(), Stability.PREVIEW);
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        // test configuration put in standalone.xml
        return readResource("grpc-subsystem-test.xml");
    }

    @Override
    protected String getSubsystemXsdPath() {
        return "schema/wildfly-grpc_preview_1_0.xsd";
    }

    protected AdditionalInitialization createAdditionalInitialization() {
        return new ManagementAdditionalInitialization(Stability.PREVIEW) {

            @Override
            protected void initializeExtraSubystemsAndModel(ExtensionRegistry extensionRegistry, Resource rootResource,
                    ManagementResourceRegistration rootRegistration, RuntimeCapabilityRegistry capabilityRegistry) {
                super.initializeExtraSubystemsAndModel(extensionRegistry, rootResource, rootRegistration,
                        capabilityRegistry);
                registerCapabilities(capabilityRegistry, "org.wildfly.weld");
            }
        };
    }

    @Test
    public void testExpressions() throws Exception {
        standardSubsystemTest("grpc-subsystem-expressions.xml", false);
    }
}

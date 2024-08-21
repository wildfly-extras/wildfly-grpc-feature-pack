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

import java.io.IOException;

import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.junit.Test;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class SubsystemTestCase extends AbstractSubsystemBaseTest {

    public SubsystemTestCase() {
        super(GrpcExtension.SUBSYSTEM_NAME, new GrpcExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        // test configuration put in standalone.xml
        return readResource("grpc-subsystem-test.xml");
    }

    @Override
    protected String getSubsystemXsdPath() {
        return "schema/grpc-subsystem_1_0.xsd";
    }

    protected AdditionalInitialization createAdditionalInitialization() {
        return AdditionalInitialization.withCapabilities("org.wildfly.weld");
    }
    
    @Test
    public void testExpressions() throws Exception {
        standardSubsystemTest("grpc-subsystem-expressions.xml", false);
    }
}

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
package org.wildfly.extension.grpc.deployment;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.jbossallxml.JBossAllXMLParser;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

public class GrpcDeploymentXMLParser
        implements XMLElementReader<GrpcDeploymentConfiguration>, JBossAllXMLParser<GrpcDeploymentConfiguration> {

    @Override
    public GrpcDeploymentConfiguration parse(final XMLExtendedStreamReader xmlExtendedStreamReader,
            final DeploymentUnit deploymentUnit) throws XMLStreamException {
        GrpcDeploymentConfiguration gdc = new MutableGrpcDeploymentConfiguration();
        readElement(xmlExtendedStreamReader, gdc);
        return gdc;
    }

    @Override
    public void readElement(final XMLExtendedStreamReader reader, final GrpcDeploymentConfiguration gdc)
            throws XMLStreamException {
        ParseUtils.requireNoAttributes(reader);

    }
}

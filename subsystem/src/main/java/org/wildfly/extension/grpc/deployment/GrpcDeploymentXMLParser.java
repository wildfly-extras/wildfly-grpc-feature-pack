/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
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

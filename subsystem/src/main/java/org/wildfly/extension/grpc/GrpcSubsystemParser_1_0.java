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

import java.util.EnumSet;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentResourceXMLParser;
import org.jboss.as.controller.PropertiesAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import static org.jboss.as.controller.PersistentResourceXMLDescription.builder;
import static org.jboss.as.controller.parsing.ParseUtils.requireAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;

/**
 * @author <a href="mailto:rsigal@redhat.com">Ron Sigal</a>
 */
public class GrpcSubsystemParser_1_0 extends PersistentResourceXMLParser {
    public static final String NAMESPACE = "urn:wildfly:grpc:1.0";

    private static final PersistentResourceXMLDescription xmlDescription = builder(
            GrpcSubsystemDefinition.INSTANCE.getPathElement(), GrpcSubsystemSchema.GRPC_1_0.getUriString()).build();

    @Override
    public PersistentResourceXMLDescription getParserDescription() {
        return xmlDescription;
    }

    @Override
    public void readElement(final XMLExtendedStreamReader reader, final List<ModelNode> list) throws XMLStreamException {
        final PathAddress address = PathAddress.pathAddress(GrpcExtension.SUBSYSTEM_PATH);
        final ModelNode subsystem = Util.createAddOperation(address);
        list.add(subsystem);
        requireNoAttributes(reader);

        final EnumSet<GrpcElement> encountered = EnumSet.noneOf(GrpcElement.class);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final GrpcElement element = GrpcElement.forName(reader.getLocalName());
            switch (element) {

                case WILDFLY_GRPC_SERVER_PORT:
                    handleSimpleElement(reader, encountered, subsystem, GrpcElement.WILDFLY_GRPC_SERVER_PORT);
                    break;

                case WILDFLY_GRPC_SERVER_HOST:
                    handleSimpleElement(reader, encountered, subsystem, GrpcElement.WILDFLY_GRPC_SERVER_HOST);
                    break;

                default:
                    throw unexpectedElement(reader);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeContent(final XMLExtendedStreamWriter streamWriter, final SubsystemMarshallingContext context)
            throws XMLStreamException {
        context.startSubsystemElement(NAMESPACE, false);
        ModelNode subsystem = context.getModelNode();
        for (AttributeDefinition attr : GrpcAttribute.ATTRIBUTES) {
            attr.marshallAsElement(subsystem, true, streamWriter);
        }
        streamWriter.writeEndElement();
    }

    protected void handleSimpleElement(final XMLExtendedStreamReader reader,
            final EnumSet<GrpcElement> encountered,
            final ModelNode subsystem,
            final GrpcElement element) throws XMLStreamException {

        if (!encountered.add(element)) {
            throw unexpectedElement(reader);
        }
        final String name = element.getLocalName();
        final String value = parseElementNoAttributes(reader);
        final SimpleAttributeDefinition attribute = (SimpleAttributeDefinition) GrpcConstants.nameToAttributeMap.get(name);
        attribute.parseAndSetParameter(value, subsystem, reader);
    }

    protected void handleList(final String tag,
            final XMLExtendedStreamReader reader,
            final EnumSet<GrpcElement> encountered,
            final ModelNode subsystem,
            final GrpcElement element) throws XMLStreamException {

        if (!encountered.add(element)) {
            throw unexpectedElement(reader);
        }
        final String name = element.getLocalName();
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            if (!tag.equals(reader.getLocalName())) {
                throw unexpectedElement(reader);
            }
            String value = parseElementNoAttributes(reader);
            subsystem.get(name).add(value);
        }
    }

    protected void handleMap(final XMLExtendedStreamReader reader,
            final EnumSet<GrpcElement> encountered,
            final ModelNode subsystem,
            final GrpcElement element) throws XMLStreamException {

        if (!encountered.add(element)) {
            throw unexpectedElement(reader);
        }
        final String name = element.getLocalName();
        final PropertiesAttributeDefinition attribute = (PropertiesAttributeDefinition) GrpcConstants.nameToAttributeMap
                .get(name);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            if (!"entry".equals(reader.getLocalName())) {
                throw unexpectedElement(reader);
            }
            final String[] array = requireAttributes(reader, "key");
            if (array.length != 1) {
                throw unexpectedElement(reader);
            }
            String value = reader.getElementText().trim();
            attribute.parseAndAddParameterElement(array[0], value, subsystem, reader);
        }
    }

    protected String parseElementNoAttributes(final XMLExtendedStreamReader reader) throws XMLStreamException {
        requireNoAttributes(reader);
        return reader.getElementText().trim();
    }
}

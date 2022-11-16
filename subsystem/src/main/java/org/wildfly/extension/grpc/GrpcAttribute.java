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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.DefaultAttributeMarshaller;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;

/**
 * gRPC configuration attributes.
 *
 * @author <a href="mailto:rsigal@redhat.com">Ron Sigal</a>
 */
public abstract class GrpcAttribute {

    public static final String GRPC_PARAMETER_GROUP = "gRPC";

    public static final SimpleAttributeDefinition WILDFLY_GRPC_SERVER_PORT = new SimpleAttributeDefinitionBuilder(
            GrpcConstants.WILDFLY_GRPC_SERVER_PORT, ModelType.INT)
            .setRequired(false)
            .setAllowExpression(true)
            .setValidator(new ModelTypeValidator(ModelType.INT, false))
            .setDefaultValue(new ModelNode(9555))
            .setAttributeGroup(GRPC_PARAMETER_GROUP)
            .build();

    public static final SimpleAttributeDefinition WILDFLY_GRPC_SERVER_HOST = new SimpleAttributeDefinitionBuilder(
            GrpcConstants.WILDFLY_GRPC_SERVER_HOST, ModelType.STRING)
            .setRequired(false)
            .setAllowExpression(true)
            .setValidator(new ModelTypeValidator(ModelType.STRING, false))
            .setDefaultValue(new ModelNode("0.0.0.0"))
            .setAttributeGroup(GRPC_PARAMETER_GROUP)
            .build();

    public static final AttributeDefinition[] ATTRIBUTES = new AttributeDefinition[] {
            WILDFLY_GRPC_SERVER_PORT,
            WILDFLY_GRPC_SERVER_HOST,
    };

    public static final AttributeDefinition[] simpleAttributesArray = new AttributeDefinition[] {
            WILDFLY_GRPC_SERVER_PORT,
            WILDFLY_GRPC_SERVER_HOST,
    };

    public static final AttributeDefinition[] listAttributeArray = new AttributeDefinition[] {
    };

    public static final AttributeDefinition[] jndiAttributesArray = new AttributeDefinition[] {
    };

    public static final AttributeDefinition[] mapAttributeArray = new AttributeDefinition[] {
    };

    public static final Set<AttributeDefinition> SIMPLE_ATTRIBUTES = new HashSet<>(Arrays.asList(simpleAttributesArray));
    public static final Set<AttributeDefinition> LIST_ATTRIBUTES = new HashSet<>(Arrays.asList(listAttributeArray));
    public static final Set<AttributeDefinition> JNDI_ATTRIBUTES = new HashSet<>(Arrays.asList(jndiAttributesArray));
    public static final Set<AttributeDefinition> MAP_ATTRIBUTES = new HashSet<>(Arrays.asList(mapAttributeArray));

//    private static class ListMarshaller extends DefaultAttributeMarshaller {
//        static final ListMarshaller INSTANCE = new ListMarshaller();
//
//        @Override
//        public void marshallAsElement(final AttributeDefinition attribute, final ModelNode resourceModel,
//                final boolean marshallDefault, final XMLStreamWriter writer) throws XMLStreamException {
//            if (isMarshallable(attribute, resourceModel, marshallDefault)) {
//                writer.writeStartElement(attribute.getXmlName());
//                List<ModelNode> list = resourceModel.get(attribute.getName()).asList();
//                for (ModelNode node : list) {
//                    String child = "class";
//                    if (JNDI_ATTRIBUTES.contains(attribute)) {
//                        child = "jndi";
//                    }
//                    writer.writeStartElement(child);
//                    writer.writeCharacters(node.asString().trim());
//                    writer.writeEndElement();
//                }
//                writer.writeEndElement();
//            }
//        }
//    }
//
//    private static class MapMarshaller extends DefaultAttributeMarshaller {
//        static final MapMarshaller INSTANCE = new MapMarshaller();
//
//        @Override
//        public void marshallAsElement(final AttributeDefinition attribute, final ModelNode resourceModel,
//                final boolean marshallDefault, final XMLStreamWriter writer) throws XMLStreamException {
//            if (isMarshallable(attribute, resourceModel, marshallDefault)) {
//                writer.writeStartElement(attribute.getXmlName());
//                List<ModelNode> list = resourceModel.get(attribute.getName()).asList();
//                for (ModelNode node : list) {
//                    Property property = node.asProperty();
//                    writer.writeStartElement("entry");
//                    writer.writeAttribute("key", property.getName());
//                    writer.writeCharacters(property.getValue().asString().trim());
//                    writer.writeEndElement();
//                }
//                writer.writeEndElement();
//            }
//        }
//    }
}

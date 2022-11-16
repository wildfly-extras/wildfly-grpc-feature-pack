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

import java.util.HashMap;
import java.util.Map;

/**
 * gRPC configuration elements.
 *
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:rsigal@redhat.com">Ron Sigal</a>
 */
enum GrpcElement {
    UNKNOWN(null), WILDFLY_GRPC_SERVER_PORT(GrpcConstants.WILDFLY_GRPC_SERVER_PORT), WILDFLY_GRPC_SERVER_HOST(
            GrpcConstants.WILDFLY_GRPC_SERVER_HOST),
            ;

    private final String name;

    private GrpcElement(final String name) {
        this.name = name;
    }

    private static final Map<String, GrpcElement> MAP;

    static {
        final Map<String, GrpcElement> map = new HashMap<String, GrpcElement>();
        for (final GrpcElement element : values()) {
            final String name = element.getLocalName();
            if (name != null)
                map.put(name, element);
        }
        MAP = map;
    }

    static GrpcElement forName(final String localName) {
        final GrpcElement element = MAP.get(localName);
        return element == null ? UNKNOWN : element;
    }

    /**
     * Get the local name of this element.
     *
     * @return the local name
     */
    String getLocalName() {
        return name;
    }
}

/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.feature.pack.grpc.test.coexistence;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

/**
 * A JAX-RS endpoint whose path mirrors the gRPC service name, used to verify that
 * non-gRPC HTTP requests are not intercepted by the gRPC routing handler.
 */
@Path("/helloworld.Greeter")
public class GreeterHttpEndpoint {

    @GET
    @Path("/SayHello")
    @Produces(MediaType.TEXT_PLAIN)
    public String sayHello(@QueryParam("name") String name) {
        return "HTTP Hello " + name;
    }
}

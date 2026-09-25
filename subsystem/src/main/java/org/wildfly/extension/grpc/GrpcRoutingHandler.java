/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.grpc;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

/**
 * An Undertow {@link HttpHandler} that routes {@code application/grpc} requests to the gRPC servlet
 * handler and passes all other requests to the next handler in the chain.
 * <p>
 * This guard is required because {@code GrpcServlet} returns HTTP 415 for non-gRPC requests and does
 * not forward to the next handler. Routing at this level ensures that no existing WildFly HTTP
 * endpoint is affected.
 */
class GrpcRoutingHandler implements HttpHandler {

    private final HttpHandler grpcHandler;
    private final HttpHandler next;

    GrpcRoutingHandler(final HttpHandler grpcHandler, final HttpHandler next) {
        this.grpcHandler = grpcHandler;
        this.next = next;
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        final String contentType = exchange.getRequestHeaders().getFirst(Headers.CONTENT_TYPE);
        if (contentType != null && contentType.startsWith("application/grpc")) {
            grpcHandler.handleRequest(exchange);
        } else {
            next.handleRequest(exchange);
        }
    }
}

/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.grpc;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityIdentity;

/**
 * Bridges the identity authenticated by the servlet container to one gRPC request.
 *
 * <p>This filter does not authenticate a request. It runs after Undertow's Elytron
 * integration, obtains the identity already associated with the servlet request, and
 * makes it available to the gRPC interceptor using an unforgeable, one-time token.
 */
public final class GrpcElytronIdentityFilter implements Filter {

    @Override
    public void init(final FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response,
            final FilterChain chain) throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest httpRequest)) {
            chain.doFilter(request, response);
            return;
        }

        final SecurityDomain domain = SecurityDomain.getCurrent();
        final SecurityIdentity identity = domain == null ? null : domain.getCurrentSecurityIdentity();
        if (identity == null) {
            chain.doFilter(request, response);
            return;
        }

        final String token = GrpcElytronIdentityContext.register(identity);
        final HttpServletRequest wrappedRequest = new TokenRequestWrapper(httpRequest, token);
        boolean async = false;
        try {
            chain.doFilter(wrappedRequest, response);
            async = wrappedRequest.isAsyncStarted();
            if (async) {
                wrappedRequest.getAsyncContext().addListener(new TokenCleanupListener(token));
            }
        } finally {
            if (!async) {
                GrpcElytronIdentityContext.discard(token);
            }
        }
    }

    @Override
    public void destroy() {
    }

    private static final class TokenCleanupListener implements AsyncListener {

        private final String token;

        private TokenCleanupListener(final String token) {
            this.token = token;
        }

        @Override
        public void onComplete(final AsyncEvent event) {
            GrpcElytronIdentityContext.discard(token);
        }

        @Override
        public void onTimeout(final AsyncEvent event) {
            GrpcElytronIdentityContext.discard(token);
        }

        @Override
        public void onError(final AsyncEvent event) {
            GrpcElytronIdentityContext.discard(token);
        }

        @Override
        public void onStartAsync(final AsyncEvent event) {
        }
    }

    private static final class TokenRequestWrapper extends HttpServletRequestWrapper {

        private final String token;

        private TokenRequestWrapper(final HttpServletRequest request, final String token) {
            super(request);
            this.token = token;
        }

        @Override
        public String getHeader(final String name) {
            return GrpcElytronIdentityContext.HEADER_NAME.equalsIgnoreCase(name)
                    ? token : super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(final String name) {
            return GrpcElytronIdentityContext.HEADER_NAME.equalsIgnoreCase(name)
                    ? Collections.enumeration(Collections.singleton(token)) : super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            final Set<String> names = new LinkedHashSet<>();
            final Enumeration<String> headers = super.getHeaderNames();
            while (headers.hasMoreElements()) {
                final String name = headers.nextElement();
                if (!GrpcElytronIdentityContext.HEADER_NAME.equalsIgnoreCase(name)) {
                    names.add(name);
                }
            }
            names.add(GrpcElytronIdentityContext.HEADER_NAME);
            return Collections.enumeration(names);
        }
    }
}

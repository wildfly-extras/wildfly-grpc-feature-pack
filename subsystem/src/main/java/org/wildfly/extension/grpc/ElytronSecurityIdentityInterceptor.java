/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.grpc;

import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityIdentity;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

/**
 * Built-in {@link ServerInterceptor} that propagates the Elytron {@link SecurityIdentity}
 * from the current thread into the gRPC {@link Context}.
 * <p>
 * WildFly's security infrastructure associates the identity with the thread before the
 * servlet dispatch, so {@link SecurityDomain#getCurrent()} is non-null when this interceptor
 * runs. The identity is then available to gRPC service methods via
 * {@link GrpcSecurity#SECURITY_IDENTITY_KEY}.
 * <p>
 * This interceptor is prepended as the outermost interceptor for every registered gRPC
 * service so it runs before any user-provided interceptors.
 */
final class ElytronSecurityIdentityInterceptor implements ServerInterceptor {

    static final ElytronSecurityIdentityInterceptor INSTANCE = new ElytronSecurityIdentityInterceptor();

    private ElytronSecurityIdentityInterceptor() {
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        final SecurityDomain domain = SecurityDomain.getCurrent();
        if (domain != null) {
            final SecurityIdentity identity = domain.getCurrentSecurityIdentity();
            if (identity != null) {
                final Context ctx = Context.current()
                        .withValue(GrpcSecurity.SECURITY_IDENTITY_KEY, identity);
                return Contexts.interceptCall(ctx, call, headers, next);
            }
        }
        return next.startCall(call, headers);
    }
}

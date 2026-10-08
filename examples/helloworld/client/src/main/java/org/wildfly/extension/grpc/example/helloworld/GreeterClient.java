/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.grpc.example.helloworld;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;

public class GreeterClient {

    private static final Logger logger = Logger.getLogger(GreeterClient.class.getName());

    private final GreeterGrpc.GreeterBlockingStub blockingStub;

    public GreeterClient(Channel channel) {
        blockingStub = GreeterGrpc.newBlockingStub(channel);
    }

    public void greet(String name) {
        logger.info("Will try to greet " + name + " ...");
        HelloRequest request = HelloRequest.newBuilder().setName(name).build();
        HelloReply response;
        try {
            response = blockingStub.sayHello(request);
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
            return;
        }
        logger.info("Greeting: " + response.getMessage());
    }

    public static void main(String[] args) throws Exception {
        String sslArg = "none";
        String name = "world";

        for (int i = 0; i < args.length; i++) {
            if ("--help".equals(args[i]) || "-h".equals(args[i])) {
                System.err.println("Usage: [options] [name]");
                System.err.println("  name                  name to greet (default: world)");
                System.err.println("  --ssl=<mode>          none, oneway, or twoway (default: none)");
                System.exit(1);
            } else if (args[i].startsWith("--ssl=")) {
                sslArg = args[i].substring("--ssl=".length());
            } else if (args[i].startsWith("-")) {
                System.err.println("Unknown option: " + args[i]);
                System.err.println("Try --help for usage.");
                System.exit(1);
            } else {
                name = args[i];
            }
        }

        SslMode sslMode = SslMode.fromString(sslArg);
        ManagedChannel channel = sslMode.createChannel();
        try {
            GreeterClient client = new GreeterClient(channel);
            client.greet(name);
        } finally {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}

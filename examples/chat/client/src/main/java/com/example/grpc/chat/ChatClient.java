/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package com.example.grpc.chat;

import java.io.InputStream;
import java.time.Duration;
import java.util.concurrent.CopyOnWriteArrayList;

import org.wildfly.extension.grpc.example.chat.ChatMessage;
import org.wildfly.extension.grpc.example.chat.ChatMessageFromServer;
import org.wildfly.extension.grpc.example.chat.ChatServiceGrpc;

import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Modifier;
import dev.tamboui.style.Style;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.TuiRunner;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.TickEvent;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.block.Title;
import dev.tamboui.widgets.list.ListItem;
import dev.tamboui.widgets.list.ListState;
import dev.tamboui.widgets.list.ListWidget;
import dev.tamboui.widgets.paragraph.Paragraph;
import io.grpc.ChannelCredentials;
import io.grpc.Grpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.TlsChannelCredentials;
import io.grpc.stub.StreamObserver;

public class ChatClient {

    public static void main(String[] args) throws Exception {
        String name = "user";
        String ssl = "none";

        if (args.length > 0) {
            if ("--help".equals(args[0])) {
                System.err.println("Usage: [name] [ssl]");
                System.err.println("  name    display name (default: user)");
                System.err.println("  ssl     none, oneway, or twoway (default: none)");
                System.exit(1);
            }
            name = args[0];
        }
        if (args.length > 1) {
            ssl = args[1];
        }

        ManagedChannel channel = createChannel(ssl);
        try {
            run(channel, name);
        } finally {
            channel.shutdown();
        }
    }

    private static void run(ManagedChannel channel, String name) throws Exception {
        CopyOnWriteArrayList<String> messages = new CopyOnWriteArrayList<>();
        StringBuilder inputBuf = new StringBuilder();
        ListState listState = new ListState();

        ChatServiceGrpc.ChatServiceStub chatService = ChatServiceGrpc.newStub(channel);
        StreamObserver<ChatMessage> outgoingStream = chatService.chat(new StreamObserver<>() {
            @Override
            public void onNext(ChatMessageFromServer value) {
                messages.add(value.getMessage().getFrom() + ": " + value.getMessage().getMessage());
            }

            @Override
            public void onError(Throwable t) {
                messages.add("[error: " + t.getMessage() + "]");
            }

            @Override
            public void onCompleted() {
                messages.add("[disconnected]");
            }
        });

        var config = TuiConfig.builder()
                .tickRate(Duration.ofMillis(100))
                .build();

        try (var tui = TuiRunner.create(config)) {
            tui.run(
                    (event, runner) -> {
                        if (event instanceof KeyEvent k) {
                            if (k.isQuit()) {
                                outgoingStream.onCompleted();
                                runner.quit();
                                return true;
                            } else if (k.code() == KeyCode.ENTER) {
                                String text = inputBuf.toString().trim();
                                if (!text.isEmpty()) {
                                    outgoingStream.onNext(ChatMessage.newBuilder()
                                            .setFrom(name)
                                            .setMessage(text)
                                            .build());
                                    inputBuf.setLength(0);
                                }
                                return true;
                            } else if (k.code() == KeyCode.BACKSPACE) {
                                if (!inputBuf.isEmpty()) {
                                    inputBuf.deleteCharAt(inputBuf.length() - 1);
                                }
                                return true;
                            } else if (k.code() == KeyCode.CHAR) {
                                inputBuf.append(k.string());
                                return true;
                            }
                        } else if (event instanceof TickEvent) {
                            return true;
                        }
                        return false;
                    },
                    frame -> {
                        var chunks = Layout.vertical()
                                .constraints(Constraint.fill(), Constraint.length(3))
                                .split(frame.area());
                        Rect historyArea = chunks.get(0);
                        Rect inputArea = chunks.get(1);

                        var items = messages.stream()
                                .map(ListItem::from)
                                .toArray(ListItem[]::new);

                        if (items.length > 0) {
                            listState.selectLast(items.length);
                        }

                        var listWidget = ListWidget.builder()
                                .items(items)
                                .block(Block.builder()
                                        .title(Title.from(" gRPC Chat — " + name + " "))
                                        .borders(Borders.ALL)
                                        .borderType(BorderType.ROUNDED)
                                        .build())
                                .build();
                        frame.renderStatefulWidget(listWidget, historyArea, listState);

                        var inputParagraph = Paragraph.builder()
                                .text(Text.from(Line.from(
                                        Span.styled("> ", Style.EMPTY.fg(Color.CYAN).addModifier(Modifier.BOLD)),
                                        Span.raw(inputBuf.toString()))))
                                .block(Block.builder()
                                        .title(Title.from(" Message (Enter to send • Ctrl+C to quit) "))
                                        .borders(Borders.ALL)
                                        .build())
                                .build();
                        frame.renderWidget(inputParagraph, inputArea);
                    });
        }
    }

    private static ManagedChannel createChannel(String ssl) throws Exception {
        String target = "localhost:9555";
        ClassLoader classLoader = ChatClient.class.getClassLoader();

        if ("oneway".equals(ssl)) {
            InputStream trustStore = classLoader.getResourceAsStream("client.truststore.pem");
            ChannelCredentials creds = TlsChannelCredentials.newBuilder().trustManager(trustStore).build();
            return Grpc.newChannelBuilderForAddress("localhost", 9555, creds).build();
        } else if ("twoway".equals(ssl)) {
            InputStream trustStore = classLoader.getResourceAsStream("client.truststore.pem");
            InputStream keyStore = classLoader.getResourceAsStream("client.keystore.pem");
            InputStream key = classLoader.getResourceAsStream("client.key.pem");
            ChannelCredentials creds = TlsChannelCredentials.newBuilder()
                    .trustManager(trustStore)
                    .keyManager(keyStore, key)
                    .build();
            return Grpc.newChannelBuilderForAddress("localhost", 9555, creds).build();
        } else if (!"none".equals(ssl)) {
            throw new IllegalArgumentException("unrecognized ssl value: " + ssl);
        }
        return ManagedChannelBuilder.forTarget(target).usePlaintext().build();
    }
}

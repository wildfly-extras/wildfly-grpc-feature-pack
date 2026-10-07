/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package com.example.grpc.chat;

import java.time.Duration;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.wildfly.extension.grpc.example.chat.ChatMessage;
import org.wildfly.extension.grpc.example.chat.ChatMessageFromServer;
import org.wildfly.extension.grpc.example.chat.ChatServiceGrpc;

import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.TuiRunner;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.TickEvent;
import dev.tamboui.widgets.input.TextInputState;
import dev.tamboui.widgets.list.ListState;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;

import static java.util.concurrent.TimeUnit.SECONDS;

public class ChatClient {

    private final ManagedChannel channel;
    private final ChatView view;
    private final ChatState state;
    private final AtomicInteger lastRenderedCount;
    private StreamObserver<ChatMessage> chatStream;

    public ChatClient(SslMode sslMode, String username, ManagedChannel channel) {
        this.channel = channel;
        this.view = new ChatView(new ChatTheme());
        this.state = new ChatState(
                new CopyOnWriteArrayList<>(),
                new ListState(),
                new TextInputState(username),
                new TextInputState(),
                sslMode);
        this.lastRenderedCount = new AtomicInteger(0);
    }

    public static void main(String[] args) throws Exception {
        String sslArg = args.length > 0 ? args[0] : "none";
        String username = args.length > 1 && args[1] != null && !args[1].isEmpty()
                ? args[1]
                : "User" + ProcessHandle.current().pid();
        if ("--help".equals(sslArg)) {
            System.err.println("Usage: [ssl] [username]");
            System.err.println("  ssl       none, oneway, or twoway");
            System.err.println("  username  your chat name (default: User<pid>)");
            System.exit(1);
        }

        SslMode sslMode = SslMode.fromString(sslArg);
        ManagedChannel channel = sslMode.createChannel();
        ChatClient client = new ChatClient(sslMode, username, channel);
        try {
            client.run();
        } finally {
            channel.shutdown();
            if (!channel.awaitTermination(3, SECONDS)) {
                channel.shutdownNow();
            }
        }
    }

    private void run() throws Exception {
        connectToServer();

        TuiConfig config = TuiConfig.builder()
                .tickRate(Duration.ofMillis(100))
                .build();

        try (TuiRunner tui = TuiRunner.create(config)) {
            tui.run(
                    (event, runner) -> {
                        if (event instanceof KeyEvent) {
                            return handleKeyEvent((KeyEvent) event, runner);
                        }
                        if (event instanceof TickEvent) {
                            int current = state.messages.size();
                            if (current > lastRenderedCount.getAndSet(current)) {
                                state.listState.applyScrollToEnd(current,
                                        state.listState.offset() + current);
                                return true;
                            }
                            return false;
                        }
                        return false;
                    },
                    frame -> view.render(frame, state));
        } finally {
            if (chatStream != null) {
                chatStream.onCompleted();
            }
        }
    }

    private boolean handleKeyEvent(KeyEvent event, TuiRunner runner) {
        if (event.isCtrlC()) {
            runner.quit();
            return false;
        }

        if (event.isKey(KeyCode.TAB)) {
            state.nameFieldFocused = !state.nameFieldFocused;
            return true;
        }

        if (event.isKey(KeyCode.ENTER)) {
            sendMessage();
            return true;
        }

        TextInputState activeState = state.nameFieldFocused ? state.nameState : state.messageState;
        if (event.isDeleteBackward()) activeState.deleteBackward();
        else if (event.isDeleteForward()) activeState.deleteForward();
        else if (event.isLeft()) activeState.moveCursorLeft();
        else if (event.isRight()) activeState.moveCursorRight();
        else if (event.isHome()) activeState.moveCursorToStart();
        else if (event.isEnd()) activeState.moveCursorToEnd();
        else if (event.code() == KeyCode.CHAR) activeState.insert(event.string());
        else return false;
        return true;
    }

    private void sendMessage() {
        String text = state.messageState.text();
        String name = state.nameState.text();
        if (text.isEmpty() || name.isEmpty()) {
            return;
        }
        if (chatStream != null) {
            chatStream.onNext(ChatMessage.newBuilder()
                    .setFrom(name)
                    .setMessage(text)
                    .build());
            state.messageState.clear();
        }
    }

    private void connectToServer() {
        ChatServiceGrpc.ChatServiceStub chatService = ChatServiceGrpc.newStub(channel);
        chatStream = chatService.chat(new StreamObserver<>() {
            @Override
            public void onNext(ChatMessageFromServer value) {
                state.messages.add(value);
            }

            @Override
            public void onError(Throwable t) {
                state.connected = false;
                state.errorMessage = t.getMessage();
            }

            @Override
            public void onCompleted() {
                state.connected = false;
            }
        });
        state.connected = true;
    }
}

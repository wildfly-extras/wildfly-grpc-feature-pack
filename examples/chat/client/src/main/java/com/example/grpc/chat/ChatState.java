/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package com.example.grpc.chat;

import java.util.List;

import org.wildfly.extension.grpc.example.chat.ChatMessageFromServer;

import dev.tamboui.widgets.input.TextInputState;
import dev.tamboui.widgets.list.ListState;

class ChatState {

    final List<ChatMessageFromServer> messages;
    final ListState listState;
    final TextInputState nameState;
    final TextInputState messageState;
    final SslMode sslMode;
    boolean nameFieldFocused;
    volatile ConnectionState connectionState;
    volatile boolean connectionStateChanged;
    volatile String errorMessage;

    ChatState(List<ChatMessageFromServer> messages, ListState listState,
            TextInputState nameState, TextInputState messageState, SslMode sslMode) {
        this.messages = messages;
        this.listState = listState;
        this.nameState = nameState;
        this.messageState = messageState;
        this.sslMode = sslMode;
    }
}

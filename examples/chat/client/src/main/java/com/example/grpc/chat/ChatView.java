/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package com.example.grpc.chat;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.wildfly.extension.grpc.example.chat.ChatMessageFromServer;

import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Padding;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.input.TextInput;
import dev.tamboui.widgets.input.TextInputState;
import dev.tamboui.widgets.list.ListItem;
import dev.tamboui.widgets.list.ListWidget;
import dev.tamboui.widgets.list.ScrollMode;
import dev.tamboui.widgets.paragraph.Paragraph;

class ChatView {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ChatTheme theme;

    ChatView(ChatTheme theme) {
        this.theme = theme;
    }

    void render(Frame frame, ChatState state) {
        Rect area = frame.area();

        List<Rect> mainLayout = Layout.vertical()
                .constraints(
                        Constraint.length(3),
                        Constraint.fill(),
                        Constraint.length(3),
                        Constraint.length(1))
                .split(area);

        renderHeader(mainLayout.get(0), frame);
        renderMessages(mainLayout.get(1), frame, state);
        renderInput(mainLayout.get(2), frame, state);
        renderStatusBar(mainLayout.get(3), frame, state);
    }

    private void renderHeader(Rect area, Frame frame) {
        Paragraph header = Paragraph.builder()
                .text(Text.from(Line.from(
                        Span.raw(" gRPC Chat ").bold().fg(theme.accent))))
                .block(Block.builder()
                        .borders(Borders.ALL)
                        .borderType(BorderType.ROUNDED)
                        .borderColor(theme.headerBorder)
                        .build())
                .build();
        frame.renderWidget(header, area);
    }

    private void renderMessages(Rect area, Frame frame, ChatState state) {
        List<ListItem> items = new ArrayList<>();
        for (ChatMessageFromServer msg : state.messages) {
            long epochSeconds = msg.getTimestamp().getSeconds();
            LocalTime time = LocalTime.ofInstant(
                    Instant.ofEpochSecond(epochSeconds), ZoneId.systemDefault());
            String timestamp = time.format(TIME_FORMAT);

            String from = msg.getMessage().getFrom();
            Color userColor = theme.colorForUser(from);

            Line line = Line.from(
                    Span.raw("[" + timestamp + "] ").fg(theme.timestamp),
                    Span.raw(from).fg(userColor).bold(),
                    Span.raw(": ").fg(theme.separator),
                    Span.raw(msg.getMessage().getMessage()));
            items.add(ListItem.from(Text.from(line)));
        }

        ListWidget list = ListWidget.builder()
                .items(items.toArray(new ListItem[0]))
                .block(Block.builder()
                        .title(" Messages ")
                        .borders(Borders.ALL)
                        .borderType(BorderType.ROUNDED)
                        .borderColor(theme.messagesBorder)
                        .build())
                .scrollMode(ScrollMode.SCROLL_TO_END)
                .highlightStyle(Style.EMPTY)
                .build();
        frame.renderStatefulWidget(list, area, state.listState);
    }

    private void renderInput(Rect area, Frame frame, ChatState state) {
        List<Rect> inputLayout = Layout.horizontal()
                .constraints(
                        Constraint.length(20),
                        Constraint.fill())
                .split(area);

        renderTextInput(frame, inputLayout.get(0), " Name ", null,
                state.nameState, state.nameFieldFocused);
        renderTextInput(frame, inputLayout.get(1), " Message ", "Type a message...",
                state.messageState, !state.nameFieldFocused);
    }

    private void renderTextInput(Frame frame, Rect area, String title, String placeholder,
            TextInputState inputState, boolean focused) {
        Color borderColor = focused ? theme.accent : theme.border;
        TextInput.Builder builder = TextInput.builder()
                .cursorColor(theme.accent)
                .block(Block.builder()
                        .title(title)
                        .borders(Borders.ALL)
                        .borderType(BorderType.ROUNDED)
                        .borderColor(borderColor)
                        .padding(new Padding(0, 1, 0, 1))
                        .build());
        if (placeholder != null) {
            builder.placeholder(placeholder);
        }
        TextInput input = builder.build();
        if (focused) {
            input.renderWithCursor(area, frame.buffer(), inputState, frame);
        } else {
            frame.renderStatefulWidget(input, area, inputState);
        }
    }

    private void renderStatusBar(Rect area, Frame frame, ChatState state) {
        String status;
        Color statusColor = switch (state.connectionState) {
            case CONNECTED -> {
                status = "Connected";
                yield theme.statusConnected;
            }
            case DISCONNECTED -> {
                status = "Disconnected";
                yield theme.statusDisconnected;
            }
            default -> {
                status = "Connecting...";
                yield theme.statusInfo;
            }
        };

        List<Span> spans = new ArrayList<>();
        spans.add(Span.raw(" " + status).fg(statusColor).bold());
        if (state.errorMessage != null) {
            spans.add(Span.raw(" — " + state.errorMessage).fg(theme.statusError));
        }
        spans.add(Span.raw(" | SSL: " + state.sslMode).fg(theme.statusInfo));
        spans.add(Span.raw(" | Tab=Switch  Enter=Send  Ctrl+C=Quit").fg(theme.statusInfo));

        Paragraph statusBar = Paragraph.builder()
                .text(Text.from(Line.from(spans)))
                .build();
        frame.renderWidget(statusBar, area);
    }
}

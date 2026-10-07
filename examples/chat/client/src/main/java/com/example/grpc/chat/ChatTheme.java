/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package com.example.grpc.chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.css.model.Stylesheet;
import dev.tamboui.css.parser.CssParser;
import dev.tamboui.style.Color;

class ChatTheme {

    final Color accent;
    final Color border;
    final Color headerBorder;
    final Color messagesBorder;
    final Color timestamp;
    final Color separator;
    final Color statusConnected;
    final Color statusDisconnected;
    final Color statusError;
    final Color statusInfo;
    final Color[] userColors;

    ChatTheme() {
        Stylesheet stylesheet = loadStylesheet();
        StyleEngine engine = StyleEngine.create();

        accent = resolveColor(stylesheet, engine, "accent", Color.CYAN);
        border = resolveColor(stylesheet, engine, "border", Color.DARK_GRAY);
        headerBorder = resolveColor(stylesheet, engine, "header-border", Color.CYAN);
        messagesBorder = resolveColor(stylesheet, engine, "messages-border", Color.GREEN);
        timestamp = resolveColor(stylesheet, engine, "timestamp", Color.DARK_GRAY);
        separator = resolveColor(stylesheet, engine, "separator", Color.DARK_GRAY);
        statusConnected = resolveColor(stylesheet, engine, "status-connected", Color.GREEN);
        statusDisconnected = resolveColor(stylesheet, engine, "status-disconnected", Color.RED);
        statusError = resolveColor(stylesheet, engine, "status-error", Color.RED);
        statusInfo = resolveColor(stylesheet, engine, "status-info", Color.DARK_GRAY);
        userColors = new Color[] {
                resolveColor(stylesheet, engine, "user-1", Color.CYAN),
                resolveColor(stylesheet, engine, "user-2", Color.YELLOW),
                resolveColor(stylesheet, engine, "user-3", Color.GREEN),
                resolveColor(stylesheet, engine, "user-4", Color.MAGENTA),
                resolveColor(stylesheet, engine, "user-5", Color.LIGHT_RED),
                resolveColor(stylesheet, engine, "user-6", Color.LIGHT_BLUE),
                resolveColor(stylesheet, engine, "user-7", Color.LIGHT_CYAN)
        };
    }

    Color colorForUser(String username) {
        int index = (username.hashCode() & 0x7FFFFFFF) % userColors.length;
        return userColors[index];
    }

    private static Stylesheet loadStylesheet() {
        try (InputStream is = ChatTheme.class.getResourceAsStream("/chat.tcss")) {
            if (is == null) {
                return Stylesheet.empty();
            }
            String css = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));
            return CssParser.parse(css);
        } catch (IOException e) {
            return Stylesheet.empty();
        }
    }

    private static Color resolveColor(Stylesheet stylesheet, StyleEngine engine,
            String variable, Color fallback) {
        return stylesheet.resolveVariable(variable)
                .flatMap(engine::parseColor)
                .orElse(fallback);
    }
}

/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package com.example.grpc.chat;

import java.io.InputStream;

import org.wildfly.extension.grpc.example.chat.ChatMessage;
import org.wildfly.extension.grpc.example.chat.ChatMessageFromServer;
import org.wildfly.extension.grpc.example.chat.ChatServiceGrpc;

import io.grpc.ChannelCredentials;
import io.grpc.Grpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.TlsChannelCredentials;
import io.grpc.stub.StreamObserver;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class ChatClient extends Application {

    private static ManagedChannel channel = null;

    private final ObservableList<String> messages = FXCollections.observableArrayList();
    private final ListView<String> messagesView = new ListView<>();
    private final TextField name = new TextField("name");
    private final TextField message = new TextField();
    private final Button send = new Button();

    public static void main(String[] args) {
        setup(args);
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        messagesView.setItems(messages);

        send.setText("Send");

        BorderPane pane = new BorderPane();
        pane.setLeft(name);
        pane.setCenter(message);
        pane.setRight(send);

        BorderPane root = new BorderPane();
        root.setCenter(messagesView);
        root.setBottom(pane);

        primaryStage.setTitle("gRPC Chat");
        primaryStage.setScene(new Scene(root, 480, 320));

        primaryStage.show();

        ChatServiceGrpc.ChatServiceStub chatService = ChatServiceGrpc.newStub(channel);
        StreamObserver<ChatMessage> chat = chatService.chat(new StreamObserver<>() {
            @Override
            public void onNext(ChatMessageFromServer value) {
                Platform.runLater(() -> {
                    messages.add(value.getMessage().getFrom() + ": " + value.getMessage().getMessage());
                    messagesView.scrollTo(messages.size());
                });
            }

            @Override
            public void onError(Throwable t) {
                t.printStackTrace();
                System.out.println("Disconnected");
            }

            @Override
            public void onCompleted() {
                System.out.println("Disconnected");
            }
        });

        send.setOnAction(e -> {
            chat.onNext(ChatMessage.newBuilder().setFrom(name.getText()).setMessage(message.getText()).build());
            message.setText("");
        });
        primaryStage.setOnCloseRequest(e -> {
            chat.onCompleted();
            channel.shutdown();
        });
    }

    private static void setup(String[] args) {
        String target = "localhost:9555";
        String ssl = "none";

        // Allow passing in the user and target strings as command line arguments
        if (args.length > 0) {
            if ("--help".equals(args[0])) {
                System.err.println("Usage: [ssl]");
                System.err.println("");
                System.err.println("  ssl     none, oneway, or twoway");
                System.exit(1);
            }
            ssl = args[0];
        }
        try {
            ClassLoader classLoader = ChatClient.class.getClassLoader();
            if ("none".equals(ssl)) {
                channel = ManagedChannelBuilder.forTarget(target)
                        // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
                        // needing certificates.
                        .usePlaintext().build();
            } else if ("oneway".equals(ssl)) {
                InputStream trustStore = classLoader.getResourceAsStream("client.truststore.pem");
                ChannelCredentials creds = TlsChannelCredentials.newBuilder().trustManager(trustStore).build();
                channel = Grpc.newChannelBuilderForAddress("localhost", 9555, creds).build();
            } else if ("twoway".equals(ssl)) {
                InputStream trustStore = classLoader.getResourceAsStream("client.truststore.pem");
                InputStream keyStore = classLoader.getResourceAsStream("client.keystore.pem");
                InputStream key = classLoader.getResourceAsStream("client.key.pem");
                ChannelCredentials creds = TlsChannelCredentials.newBuilder().trustManager(trustStore).keyManager(keyStore, key)
                        .build();
                channel = Grpc.newChannelBuilderForAddress("localhost", 9555, creds).build();
            } else {
                System.err.println("unrecognized ssl value: " + ssl);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

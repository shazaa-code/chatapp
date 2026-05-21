package controller;

import client.ChatClient;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class ChatController {

    @FXML private TextArea publicArea;
    @FXML private TextField messageField;
    @FXML private Label statusLabel;

    private ChatClient chatClient;
    private String username;

    public void init(ChatClient client, String username) {
        this.chatClient = client;
        this.username = username;
        client.setCallbacks(this::onMessage, this::onError);
        statusLabel.setText("Logged in as: " + username);
    }

    private void onMessage(String line) {
        Platform.runLater(() -> publicArea.appendText(line + "\n"));
    }

    private void onError(String msg) {
        Platform.runLater(() -> statusLabel.setText("Error: " + msg));
    }

    @FXML
    public void sendPublic() {
        String text = messageField.getText().trim();
        if (text.isEmpty() || chatClient == null) return;
        chatClient.send("PUBLIC:" + username + ":" + text);
        messageField.clear();
    }
}
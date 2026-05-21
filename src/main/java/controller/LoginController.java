package controller;

import client.ChatClient;
import exception.NetworkException;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import server.ChatServer;


public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;

    // ChatClient created WITH callbacks — this was the bug
    private ChatClient chatClient = new ChatClient(
            this::onMessage,
            this::onError
    );

    @FXML
    public void handleLogin() {
        connect("AUTH");
    }

    @FXML
    public void handleRegister() {
        connect("REGISTER");
    }

    private void connect(String command) {
        String user = usernameField.getText().trim();
        String pass = passwordField.getText();

        if (user.isEmpty() || pass.isEmpty()) {
            statusLabel.setText("Enter username and password");
            return;
        }

        try {
            chatClient.connect("localhost", ChatServer.PORT);
            chatClient.send(command + ":" + user + ":" + pass);
        } catch (NetworkException e) {
            statusLabel.setText("Network error: " + e.getMessage());
        }
    }

    private void onMessage(String line) {
        // Must update UI on JavaFX thread
        Platform.runLater(() -> {
            if (line.startsWith("OK:")) {
                String username = line.substring(3);
                openChat(username);
            } else {
                statusLabel.setText(line);
            }
        });
    }

    private void onError(String msg) {
        Platform.runLater(() -> statusLabel.setText(msg));
    }

    private void openChat(String username) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/chat.fxml")
            );

            Parent root = loader.load();

            ChatController cc = loader.getController();
            cc.init(chatClient, username);

            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(root, 800, 600));
            stage.setTitle("Chat - " + username);

        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Cannot open chat window");
        }
    }
}
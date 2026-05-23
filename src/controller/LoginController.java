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

/**
 * Controller for login.fxml.
 *
 * Responsibilities:
 *  - Validate that the form fields are not empty
 *  - Create a ChatClient, connect it to ChatServer
 *  - Send AUTH or REGISTER and wait for OK / ERROR
 *  - On OK: open chat.fxml and hand over the ChatClient
 *  - On ERROR or NetworkException: display the message in statusLabel
 */
public class LoginController {

    @FXML private TextField     usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label         statusLabel;
    @FXML private Button        loginButton;

    private ChatClient chatClient;

    // ── Button handlers ───────────────────────────────────────────────────

    @FXML
    public void handleLogin() {
        connect("AUTH");
    }

    @FXML
    public void handleRegister() {
        connect("REGISTER");
    }

    // ── Internal ──────────────────────────────────────────────────────────

    private void connect(String command) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Please enter both username and password.");
            return;
        }

        statusLabel.setText("Connecting...");
        loginButton.setDisable(true);

        try {
            // New client for each attempt
            if (chatClient != null) chatClient.disconnect();
            chatClient = new ChatClient(this::onMessage, this::onError);
            chatClient.connect("localhost", ChatServer.PORT);
            chatClient.send(command + ":" + username + ":" + password);
        } catch (NetworkException e) {
            statusLabel.setText("Network error: " + e.getMessage());
            loginButton.setDisable(false);
        }
    }

    /** Called on the JavaFX Application Thread by ChatClient's listener. */
    private void onMessage(String line) {
        if (line.startsWith("OK:")) {
            String username = line.substring(3);
            openChatWindow(username);
        } else if (line.startsWith("ERROR:")) {
            statusLabel.setText(line.substring(6));
            loginButton.setDisable(false);
            chatClient.disconnect();
        }
    }

    private void onError(String msg) {
        statusLabel.setText(msg);
        loginButton.setDisable(false);
    }

    private void openChatWindow(String username) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/chat.fxml"));
            Parent root = loader.load();

            ChatController cc = loader.getController();
            cc.init(chatClient, username);

            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(new Scene(root, 860, 600));
            stage.setTitle("JavaFX Chat  —  " + username);
            stage.setOnCloseRequest(e -> chatClient.disconnect());
        } catch (Exception e) {
            statusLabel.setText("Cannot open chat window.");
            e.printStackTrace();
        }
    }
}

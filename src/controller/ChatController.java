package controller;

import client.ChatClient;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

/**
 * Controller for chat.fxml.
 *
 * Handles:
 *  - Displaying public chat messages and history
 *  - Sending public messages
 *  - Sending and displaying private DMs
 *  - Showing system notices (join / leave)
 *
 * All UI updates arrive via Platform.runLater() from ChatClient's Task,
 * so the JavaFX Application Thread is never blocked.
 */
public class ChatController {

    @FXML private TextArea  publicArea;
    @FXML private TextArea  dmArea;
    @FXML private TextField messageField;
    @FXML private TextField dmRecipientField;
    @FXML private Label     statusLabel;
    @FXML private TabPane   tabPane;

    private ChatClient chatClient;
    private String     username;
    private boolean    inHistory = false;

    // ── Called by LoginController after successful login ─────────────────

    public void init(ChatClient client, String username) {
        this.chatClient = client;
        this.username   = username;
        statusLabel.setText("Signed in as: " + username);

        // Re-register message callback now that this controller is active
        // (LoginController's ChatClient already has the right callbacks –
        //  we just reassign via the same ChatClient instance)
    }

    // ── FXML button handlers ─────────────────────────────────────────────

    @FXML
    public void sendPublic() {
        String text = messageField.getText().trim();
        if (text.isEmpty() || chatClient == null) return;
        chatClient.send("PUBLIC:" + username + ":" + text);
        messageField.clear();
    }

    @FXML
    public void sendDM() {
        String recipient = dmRecipientField.getText().trim();
        String text      = messageField.getText().trim();
        if (recipient.isEmpty() || text.isEmpty() || chatClient == null) return;
        chatClient.send("PRIVATE:" + username + ":" + recipient + ":" + text);
        messageField.clear();
    }

    // ── Message dispatcher (called on JavaFX thread by ChatClient) ────────

    public void onMessage(String line) {
        if ("HISTORY_START".equals(line)) {
            inHistory = true;
            publicArea.appendText("── History ────────────────────────\n");
        } else if ("HISTORY_END".equals(line)) {
            inHistory = false;
            publicArea.appendText("── Live ────────────────────────────\n");
        } else if (line.startsWith("PUBLIC:")) {
            String[] p = line.split(":", 3);
            if (p.length == 3)
                publicArea.appendText("[" + p[1] + "]: " + p[2] + "\n");
        } else if (line.startsWith("PRIVATE:")) {
            String[] p = line.split(":", 4);
            if (p.length == 4)
                dmArea.appendText("[DM " + p[1] + " → " + p[2] + "]: " + p[3] + "\n");
        } else if (line.startsWith("SYSTEM::")) {
            publicArea.appendText("  " + line.substring(8) + "\n");
        }
    }

    public void onError(String msg) {
        statusLabel.setText("Error: " + msg);
    }

    /** Called externally to append any message object (used in testing). */
    public void updateChatPane(model.Message message) {
        Platform.runLater(() -> publicArea.appendText(message.toString() + "\n"));
    }
}

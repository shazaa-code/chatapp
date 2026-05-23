package client;

import exception.NetworkException;
import javafx.application.Platform;
import javafx.concurrent.Task;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * Manages the TCP connection from the JavaFX client side.
 *
 * A background {@link Task} reads incoming lines so the JavaFX Application
 * Thread is never blocked. Each received line is dispatched to
 * {@code onMessageReceived} via {@link Platform#runLater}.
 */
public class ChatClient {

    private Socket        socket;
    private PrintWriter   writer;
    private Task<Void>    listenerTask;

    private final Consumer<String> onMessageReceived;
    private final Consumer<String> onError;

    public ChatClient(Consumer<String> onMessageReceived,
                      Consumer<String> onError) {
        this.onMessageReceived = onMessageReceived;
        this.onError           = onError;
    }

    // ── Connect / disconnect ──────────────────────────────────────────────

    /**
     * Opens a TCP socket and starts the background listener task.
     *
     * @throws NetworkException if the connection cannot be established
     */
    public void connect(String host, int port) throws NetworkException {
        try {
            socket = new Socket(host, port);
            writer = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream()), true);
            startListener();
        } catch (IOException e) {
            throw new NetworkException(
                    "Cannot connect to " + host + ":" + port, e);
        }
    }

    /** Sends a raw protocol line to the server. */
    public void send(String line) {
        if (writer != null) writer.println(line);
    }

    /** Cancels the listener task and closes the socket cleanly. */
    public void disconnect() {
        if (listenerTask != null) listenerTask.cancel(true);
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    // ── Background listener task ──────────────────────────────────────────

    private void startListener() {
        listenerTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                String line;
                while (!isCancelled() && (line = reader.readLine()) != null) {
                    final String msg = line;
                    // always update UI on the JavaFX Application Thread
                    Platform.runLater(() -> onMessageReceived.accept(msg));
                }
                return null;
            }
        };

        listenerTask.setOnFailed(evt ->
            Platform.runLater(() ->
                onError.accept("Connection to server lost.")));

        Thread t = new Thread(listenerTask, "chat-listener");
        t.setDaemon(true);   // does not prevent JVM shutdown
        t.start();
    }
}

package client;

import exception.NetworkException;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

public class ChatClient {

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;

    private Consumer<String> onMessage;
    private Consumer<String> onError;

    public ChatClient(Consumer<String> onMessage,
                      Consumer<String> onError) {
        this.onMessage = onMessage;
        this.onError = onError;
    }


    public void setCallbacks(Consumer<String> onMessage, Consumer<String> onError) {
        this.onMessage = onMessage;
        this.onError = onError;
    }

    public void connect(String host, int port) throws NetworkException {
        try {
            socket = new Socket(host, port);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
            startReaderThread();
        } catch (IOException e) {
            throw new NetworkException("Cannot connect to server: " + e.getMessage());
        }
    }

    public void send(String msg) {
        if (writer != null) writer.println(msg);
    }

    private void startReaderThread() {
        Thread t = new Thread(() -> {
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (onMessage != null) onMessage.accept(line);
                }
            } catch (IOException e) {
                if (onError != null) onError.accept("Connection lost");
            }
        });
        t.setDaemon(true);
        t.start();
    }

    public void disconnect() {
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {}
    }
}
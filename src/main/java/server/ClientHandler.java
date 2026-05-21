package server;

import exception.AuthException;
import exception.DatabaseException;
import model.User;
import repository.UserRepository;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final ChatServer server;

    private BufferedReader reader;
    private PrintWriter writer;

    private String username;

    public ClientHandler(ChatServer server, Socket socket) {
        this.server = server;
        this.socket = socket;

        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            String line;
            while ((line = reader.readLine()) != null) {

                // LOGIN
                if (line.startsWith("AUTH:")) {
                    String[] parts = line.split(":");
                    if (parts.length >= 3) {
                        String user = parts[1];
                        String pass = parts[2];
                        try {
                            UserRepository repo = new UserRepository(server.getConnection());
                            repo.validateLogin(user, pass);
                            username = user;
                            writer.println("OK:" + username);
                            server.broadcast("SYSTEM::" + username + " joined the chat");
                        } catch (AuthException e) {
                            writer.println("ERROR:" + e.getMessage());
                        } catch (DatabaseException e) {
                            writer.println("ERROR:Database error");
                        }
                    }
                }

                // REGISTER
                else if (line.startsWith("REGISTER:")) {
                    String[] parts = line.split(":");
                    if (parts.length >= 3) {
                        String user = parts[1];
                        String pass = parts[2];
                        try {
                            UserRepository repo = new UserRepository(server.getConnection());
                            User newUser = new User(0, user, pass);
                            repo.register(newUser);
                            username = user;
                            writer.println("OK:" + username);
                            server.broadcast("SYSTEM::" + username + " joined the chat");
                        } catch (AuthException e) {
                            writer.println("ERROR:" + e.getMessage());
                        } catch (DatabaseException e) {
                            writer.println("ERROR:Database error");
                        }
                    }
                }

                // PUBLIC MESSAGE
                else if (line.startsWith("PUBLIC:")) {
                    server.broadcast(line);
                }

                // PRIVATE MESSAGE
                else if (line.startsWith("PRIVATE:")) {
                    String[] parts = line.split(":", 4);
                    if (parts.length == 4) {
                        String sender = parts[1];
                        String recipient = parts[2];
                        String message = parts[3];
                        server.sendPrivate(
                                recipient,
                                "PRIVATE:" + sender + ":" + recipient + ":" + message
                        );
                    }
                }
            }

        } catch (IOException e) {
            System.out.println("[Server] Client disconnected: " +
                    (username != null ? username : "unknown"));
        } finally {
            server.removeClient(this);
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    public void send(String msg) {
        if (writer != null) writer.println(msg);
    }

    public String getUsername() {
        return username;
    }
}
package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ChatServer {

    public static final int PORT = 9090;

    private final List<ClientHandler> clients = new ArrayList<>();

    private Connection conn;
    public Connection getConnection() {
        return conn;
    }

    public void start() {

        try {

            // editt
            conn = DriverManager.getConnection("jdbc:sqlite:C:/Users/shaza/IdeaProjects/chatapp/chatapp.db");

            Statement stmt = conn.createStatement();

            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS users(
                        username TEXT PRIMARY KEY,
                        password TEXT NOT NULL
                    )
                    """);

            System.out.println("[Server] Database connected.");

            // Server socket
            ServerSocket serverSocket = new ServerSocket(PORT);

            System.out.println("[Server] Running on port " + PORT);

            while (true) {

                Socket socket = serverSocket.accept();

                System.out.println("[Server] New client connected.");

                // ✔ FIXED LINE (ONLY 2 PARAMETERS)
                ClientHandler client = new ClientHandler(this, socket);

                clients.add(client);

                new Thread(client).start();
            }

        } catch (Exception e) {

            System.out.println("[Server] Fatal error.");
            e.printStackTrace();
        }
    }

    // PUBLIC CHAT
    public void broadcast(String message) {
        for (ClientHandler client : clients) {
            client.send(message);
        }
    }

    // PRIVATE CHAT
    public void sendPrivate(String username, String message) {
        for (ClientHandler client : clients) {

            if (client.getUsername() != null &&
                    client.getUsername().equals(username)) {

                client.send(message);
            }
        }
    }

    // REMOVE CLIENT
    public void removeClient(ClientHandler client) {
        clients.remove(client);
    }

    public static void main(String[] args) {
        new ChatServer().start();
    }
}
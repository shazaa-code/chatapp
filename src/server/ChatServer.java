package server;

import exception.DatabaseException;
import repository.MessageRepository;
import repository.UserRepository;
import service.MessageService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Central TCP server.
 *
 * Lifecycle:
 *   1. start() opens the ServerSocket and initialises the SQLite database.
 *   2. For each incoming connection, a new ClientHandler thread is spawned.
 *   3. broadcast() / sendPrivate() are called by ClientHandler to route messages.
 */
public class ChatServer {

    public static final int    PORT   = 9090;
    public static final String DB_URL = "jdbc:sqlite:chatapp.db";

    /** Thread-safe list – ClientHandler threads add/remove themselves. */
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    // ── Entry point ───────────────────────────────────────────────────────

    public void start() throws IOException, DatabaseException {
        // initialise database
        Connection conn;
        try {
            conn = DriverManager.getConnection(DB_URL);
            conn.setAutoCommit(true);
        } catch (SQLException e) {
            throw new DatabaseException("Cannot open database at " + DB_URL, e);
        }

        UserRepository    userRepo = new UserRepository(conn);
        MessageRepository msgRepo  = new MessageRepository(conn);
        MessageService    svc      = new MessageService(userRepo, msgRepo);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("[Server] Listening on port " + PORT);

            //noinspection InfiniteLoopStatement
            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket, this, userRepo, svc);
                clients.add(handler);
                new Thread(handler, "client-" + socket.getPort()).start();
                System.out.println("[Server] Client connected: " + socket.getInetAddress());
            }
        }
    }

    // ── Routing helpers called by ClientHandler ───────────────────────────

    /** Sends {@code line} to every connected client. */
    public void broadcast(String line) {
        for (ClientHandler c : clients) c.sendLine(line);
    }

    /**
     * Sends {@code line} to the single client whose username matches
     * {@code recipientUsername}. Silently drops if the user is offline.
     */
    public void sendPrivate(String recipientUsername, String line) {
        for (ClientHandler c : clients) {
            if (recipientUsername.equals(c.getUsername())) {
                c.sendLine(line);
                return;
            }
        }
    }

    /** Called by ClientHandler.disconnect() to remove itself. */
    public void removeClient(ClientHandler handler) {
        clients.remove(handler);
    }

    // ── main ─────────────────────────────────────────────────────────────

    public static void main(String[] args) {
        try {
            new ChatServer().start();
        } catch (Exception e) {
            System.err.println("[Server] Fatal: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

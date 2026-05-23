package server;

import exception.AuthException;
import exception.DatabaseException;
import model.Message;
import model.MessageType;
import model.User;
import repository.UserRepository;
import service.MessageService;

import java.io.*;
import java.net.Socket;
import java.util.List;

/**
 * Manages the server side of one client connection.
 *
 * Protocol handshake on connect:
 *   Client → Server:   AUTH:username:password
 *                   or REGISTER:username:password
 *   Server → Client:   OK:username
 *                   or ERROR:reason
 *
 * After OK the client receives message history, then normal chat traffic.
 */
public class ClientHandler implements Runnable {

    private final Socket         socket;
    private final ChatServer     server;
    private final UserRepository userRepo;
    private final MessageService svc;

    private BufferedReader reader;
    private PrintWriter    writer;
    private User           user;

    public ClientHandler(Socket socket, ChatServer server,
                         UserRepository userRepo, MessageService svc) {
        this.socket   = socket;
        this.server   = server;
        this.userRepo = userRepo;
        this.svc      = svc;
    }

    // ── Runnable ──────────────────────────────────────────────────────────

    @Override
    public void run() {
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

            // Step 1 – authenticate
            String authLine = reader.readLine();
            if (authLine == null || !handleAuth(authLine)) {
                disconnect();
                return;
            }

            // Step 2 – push history
            pushHistory();

            // Step 3 – notify room
            server.broadcast("SYSTEM::-- " + user.getUsername() + " joined the chat --");

            // Step 4 – main read loop
            String line;
            while ((line = reader.readLine()) != null) {
                handleIncoming(line);
            }

        } catch (IOException e) {
            System.out.println("[ClientHandler] Lost connection: "
                    + (user != null ? user.getUsername() : "?"));
        } catch (DatabaseException e) {
            System.err.println("[ClientHandler] DB error: " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    // ── Auth ──────────────────────────────────────────────────────────────

    private boolean handleAuth(String line) throws DatabaseException {
        String[] parts = line.split(":", 3);
        if (parts.length < 3) {
            sendLine("ERROR:Malformed auth line");
            return false;
        }
        String cmd      = parts[0];
        String username = parts[1];
        String password = parts[2];
        try {
            if ("AUTH".equalsIgnoreCase(cmd)) {
                user = userRepo.validateLogin(username, password);
            } else if ("REGISTER".equalsIgnoreCase(cmd)) {
                userRepo.register(new User(username, password));
                user = userRepo.validateLogin(username, password);
            } else {
                sendLine("ERROR:Unknown command " + cmd);
                return false;
            }
            sendLine("OK:" + user.getUsername());
            return true;
        } catch (AuthException e) {
            sendLine("ERROR:" + e.getMessage());
            return false;
        }
    }

    // ── History ───────────────────────────────────────────────────────────

    private void pushHistory() throws DatabaseException {
        List<Message> history = svc.loadHistory();
        sendLine("HISTORY_START");
        for (Message m : history) sendLine(m.toProtocolString());
        sendLine("HISTORY_END");
    }

    // ── Incoming messages ────────────────────────────────────────────────

    private void handleIncoming(String line) throws DatabaseException {
        MessageService.ParsedProtocol p = MessageService.parse(line);
        if (p == null) return;

        Message msg = new Message(user, p.content, p.type);

        switch (p.type) {
            case PUBLIC:
                svc.save(msg);
                server.broadcast(msg.toProtocolString());
                break;

            case PRIVATE:
                if (p.recipient == null || p.recipient.isEmpty()) return;
                msg.setRecipient(p.recipient);
                svc.save(msg);
                server.sendPrivate(p.recipient, msg.toProtocolString());
                sendLine(msg.toProtocolString());   // echo back to sender
                break;

            default:
                break;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    public void sendLine(String line) {
        if (writer != null) writer.println(line);
    }

    public void disconnect() {
        try { if (socket != null && !socket.isClosed()) socket.close(); }
        catch (IOException ignored) {}
        server.removeClient(this);
        if (user != null) {
            server.broadcast("SYSTEM::-- " + user.getUsername() + " left the chat --");
        }
    }

    public String getUsername() {
        return user != null ? user.getUsername() : null;
    }
}

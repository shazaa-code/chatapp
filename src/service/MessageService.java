package service;

import exception.DatabaseException;
import model.Message;
import model.MessageType;
import repository.MessageRepository;
import repository.UserRepository;

import java.util.List;

/**
 * Business-logic layer that sits between the network layer (ClientHandler)
 * and the persistence layer (repositories).
 *
 * Responsibilities:
 *  - persist incoming messages via MessageRepository
 *  - provide history to newly connected clients
 *  - parse raw protocol strings into ParsedProtocol objects
 */
public class MessageService {

    private final UserRepository    userRepo;
    private final MessageRepository msgRepo;

    public MessageService(UserRepository userRepo, MessageRepository msgRepo) {
        this.userRepo = userRepo;
        this.msgRepo  = msgRepo;
    }

    // ── Persistence ───────────────────────────────────────────────────────

    /** Saves any message type to the database. */
    public void save(Message message) throws DatabaseException {
        msgRepo.save(message);
    }

    /** Returns the last 50 PUBLIC messages for history replay. */
    public List<Message> loadHistory() throws DatabaseException {
        return msgRepo.getHistory(50);
    }

    /** Returns the DM thread between two users. */
    public List<Message> loadDMHistory(String a, String b) throws DatabaseException {
        return msgRepo.getDMHistory(a, b);
    }

    // ── Protocol parser ───────────────────────────────────────────────────

    /**
     * Parses a raw protocol line into a {@link ParsedProtocol} object.
     *
     * Expected formats:
     *   PUBLIC:senderName:content
     *   PRIVATE:senderName:recipientName:content
     *   SYSTEM::content
     *
     * Returns {@code null} if the line is malformed.
     */
    public static ParsedProtocol parse(String line) {
        if (line == null || line.isEmpty()) return null;
        String[] parts = line.split(":", 4);
        if (parts.length < 2) return null;
        try {
            MessageType type = MessageType.valueOf(parts[0]);
            ParsedProtocol p = new ParsedProtocol();
            p.type = type;
            switch (type) {
                case PUBLIC:
                    if (parts.length < 3) return null;
                    p.sender  = parts[1];
                    p.content = parts[2];
                    break;
                case PRIVATE:
                    if (parts.length < 4) return null;
                    p.sender    = parts[1];
                    p.recipient = parts[2];
                    p.content   = parts[3];
                    break;
                case SYSTEM:
                    p.content = parts.length >= 3 ? parts[2] : "";
                    break;
            }
            return p;
        } catch (IllegalArgumentException e) {
            return null;   // unknown type prefix
        }
    }

    /** Plain data holder for the result of {@link #parse}. */
    public static class ParsedProtocol {
        public MessageType type;
        public String      sender;
        public String      recipient;
        public String      content;
    }
}

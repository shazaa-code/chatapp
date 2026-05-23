package repository;

import exception.DatabaseException;
import model.Message;
import model.MessageType;
import model.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles all database operations for chat messages.
 * Every connection / statement uses try-with-resources.
 */
public class MessageRepository {

    private final Connection connection;

    public MessageRepository(Connection connection) throws DatabaseException {
        this.connection = connection;
        createTableIfAbsent();
    }

    // ── DDL ──────────────────────────────────────────────────────────────

    private void createTableIfAbsent() throws DatabaseException {
        String sql =
            "CREATE TABLE IF NOT EXISTS messages (" +
            "  id        INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  sender_id INTEGER NOT NULL," +
            "  content   TEXT    NOT NULL," +
            "  timestamp TEXT    NOT NULL," +
            "  type      TEXT    NOT NULL," +
            "  recipient TEXT," +
            "  FOREIGN KEY(sender_id) REFERENCES users(id)" +
            ")";
        try (Statement st = connection.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            throw new DatabaseException("Cannot create messages table", e);
        }
    }

    // ── CREATE ────────────────────────────────────────────────────────────

    /** Persists a message to the database. */
    public void save(Message msg) throws DatabaseException {
        String sql =
            "INSERT INTO messages (sender_id, content, timestamp, type, recipient)" +
            " VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt   (1, msg.getSender().getId());
            ps.setString(2, msg.getContent());
            ps.setString(3, msg.getTimestamp().toString());
            ps.setString(4, msg.getType().name());
            ps.setString(5, msg.getRecipient());   // null for PUBLIC
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("save() failed", e);
        }
    }

    // ── READ ─────────────────────────────────────────────────────────────

    /**
     * Returns the last {@code limit} PUBLIC messages in chronological order.
     * Used to populate the chat pane when a client connects.
     */
    public List<Message> getHistory(int limit) throws DatabaseException {
        String sql =
            "SELECT m.id, u.id AS uid, u.username, m.content, m.timestamp, m.type" +
            " FROM messages m JOIN users u ON m.sender_id = u.id" +
            " WHERE m.type = 'PUBLIC'" +
            " ORDER BY m.timestamp DESC LIMIT ?";
        List<Message> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    User sender = new User(rs.getInt("uid"),
                                          rs.getString("username"), "");
                    Message m = new Message(
                        rs.getInt("id"), sender,
                        rs.getString("content"),
                        LocalDateTime.parse(rs.getString("timestamp")),
                        MessageType.valueOf(rs.getString("type")));
                    list.add(0, m);   // reverse DESC → chronological
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("getHistory() failed", e);
        }
        return list;
    }

    /**
     * Returns all PRIVATE messages between two users (either direction),
     * ordered oldest-first.
     */
    public List<Message> getDMHistory(String userA, String userB)
            throws DatabaseException {
        String sql =
            "SELECT m.id, u.id AS uid, u.username, m.content," +
            "       m.timestamp, m.type, m.recipient" +
            " FROM messages m JOIN users u ON m.sender_id = u.id" +
            " WHERE m.type = 'PRIVATE'" +
            "   AND ((u.username = ? AND m.recipient = ?)" +
            "     OR (u.username = ? AND m.recipient = ?))" +
            " ORDER BY m.timestamp ASC";
        List<Message> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, userA); ps.setString(2, userB);
            ps.setString(3, userB); ps.setString(4, userA);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    User sender = new User(rs.getInt("uid"),
                                          rs.getString("username"), "");
                    Message m = new Message(
                        rs.getInt("id"), sender,
                        rs.getString("content"),
                        LocalDateTime.parse(rs.getString("timestamp")),
                        MessageType.valueOf(rs.getString("type")));
                    m.setRecipient(rs.getString("recipient"));
                    list.add(m);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("getDMHistory() failed", e);
        }
        return list;
    }
}

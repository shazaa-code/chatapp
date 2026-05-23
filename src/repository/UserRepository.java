package repository;

import exception.AuthException;
import exception.DatabaseException;
import model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles all database operations for User accounts.
 * Uses try-with-resources on every Statement / ResultSet.
 */
public class UserRepository {

    private final Connection connection;

    public UserRepository(Connection connection) throws DatabaseException {
        this.connection = connection;
        createTableIfAbsent();
    }

    // ── DDL ──────────────────────────────────────────────────────────────

    private void createTableIfAbsent() throws DatabaseException {
        String sql =
            "CREATE TABLE IF NOT EXISTS users (" +
            "  id            INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  username      TEXT    NOT NULL UNIQUE," +
            "  password_hash TEXT    NOT NULL" +
            ")";
        try (Statement st = connection.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            throw new DatabaseException("Cannot create users table", e);
        }
    }

    // ── CREATE ────────────────────────────────────────────────────────────

    /**
     * Inserts a new user row.
     * @throws AuthException     if the username already exists
     * @throws DatabaseException on any other SQL error
     */
    public void register(User user) throws DatabaseException, AuthException {
        String sql = "INSERT INTO users (username, password_hash) VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.executeUpdate();
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE")) {
                throw new AuthException("Username '" + user.getUsername() + "' is already taken.");
            }
            throw new DatabaseException("register() failed", e);
        }
    }

    // ── READ ─────────────────────────────────────────────────────────────

    /**
     * Finds a user by username; returns {@code null} if not found.
     */
    public User findByUsername(String username) throws DatabaseException {
        String sql = "SELECT id, username, password_hash FROM users WHERE username = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new User(rs.getInt("id"),
                                    rs.getString("username"),
                                    rs.getString("password_hash"));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("findByUsername() failed", e);
        }
        return null;
    }

    /**
     * Validates credentials.
     * @throws AuthException if the username does not exist or the password is wrong
     */
    public User validateLogin(String username, String password)
            throws DatabaseException, AuthException {
        User user = findByUsername(username);
        if (user == null || !user.checkPassword(password)) {
            throw new AuthException("Invalid username or password.");
        }
        return user;
    }

    /** Returns all registered usernames (used to populate online-user lists). */
    public List<String> getAllUsernames() throws DatabaseException {
        List<String> names = new ArrayList<>();
        String sql = "SELECT username FROM users ORDER BY username";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) names.add(rs.getString("username"));
        } catch (SQLException e) {
            throw new DatabaseException("getAllUsernames() failed", e);
        }
        return names;
    }
}

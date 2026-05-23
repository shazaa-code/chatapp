package model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * A registered user account.
 * Passwords are never stored in plain text – only their SHA-256 hash.
 */
public class User extends AbstractEntity {

    private final String username;
    private final String passwordHash;

    /** Reconstruct from database row (hash already stored). */
    public User(int id, String username, String passwordHash) {
        super(id);
        this.username     = username;
        this.passwordHash = passwordHash;
    }

    /** Create a brand-new user – plain password is hashed immediately. */
    public User(String username, String plainPassword) {
        this.username     = username;
        this.passwordHash = hash(plainPassword);
    }

    public String getUsername()     { return username; }
    public String getPasswordHash() { return passwordHash; }

    /** Returns true if the supplied plain password matches the stored hash. */
    public boolean checkPassword(String plainPassword) {
        return hash(plainPassword).equals(this.passwordHash);
    }

    /** SHA-256 hex digest. */
    public static String hash(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 unavailable", e);
        }
    }

    @Override
    public String toString() {
        return "User{id=" + id + ", username='" + username + "'}";
    }
}

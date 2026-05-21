package model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class User extends AbstractEntity {

    private final String username;
    private final String passwordHash;


    public User(int id, String username, String passwordHash) {
        super(id);
        this.username     = username;
        this.passwordHash = passwordHash;
    }


    public User(String username, String plainPassword) {
        this.username     = username;
        this.passwordHash = hash(plainPassword);
    }

    public String getUsername()     { return username; }
    public String getPasswordHash() { return passwordHash; }


    public boolean checkPassword(String plainPassword) {
        return hash(plainPassword).equals(this.passwordHash);
    }


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

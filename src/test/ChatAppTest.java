package test;

import exception.AuthException;
import exception.DatabaseException;
import model.Message;
import model.MessageType;
import model.User;
import org.junit.jupiter.api.*;
import repository.MessageRepository;
import repository.UserRepository;
import service.MessageService;

import java.sql.Connection;
import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 test suite.
 *
 * Uses an in-memory SQLite database (:memory:) so tests are fully isolated
 * and leave no files on disk.
 *
 * Covers:
 *  - UserRepository: register, duplicate, wrong password
 *  - MessageService.parse(): PUBLIC, PRIVATE, SYSTEM, malformed
 *  - Message.toProtocolString() round-trip
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ChatAppTest {

    private static Connection       conn;
    private static UserRepository   userRepo;
    private static MessageRepository msgRepo;
    private static MessageService   svc;

    // ── Setup / Teardown ──────────────────────────────────────────────────

    @BeforeAll
    static void setup() throws Exception {
        conn     = DriverManager.getConnection("jdbc:sqlite::memory:");
        userRepo = new UserRepository(conn);
        msgRepo  = new MessageRepository(conn);
        svc      = new MessageService(userRepo, msgRepo);
    }

    @AfterAll
    static void teardown() throws Exception {
        if (conn != null) conn.close();
    }

    // ── UserRepository ────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("Register: new user succeeds")
    void registerNewUser() {
        assertDoesNotThrow(() ->
                userRepo.register(new User("alice", "pass123")));
    }

    @Test
    @Order(2)
    @DisplayName("Register: duplicate username throws AuthException")
    void registerDuplicateThrows() {
        assertThrows(AuthException.class, () ->
                userRepo.register(new User("alice", "other")));
    }

    @Test
    @Order(3)
    @DisplayName("Login: correct credentials return User")
    void loginSuccess() throws Exception {
        User u = userRepo.validateLogin("alice", "pass123");
        assertNotNull(u);
        assertEquals("alice", u.getUsername());
    }

    @Test
    @Order(4)
    @DisplayName("Login: wrong password throws AuthException")
    void loginWrongPassword() {
        assertThrows(AuthException.class, () ->
                userRepo.validateLogin("alice", "wrong"));
    }

    @Test
    @Order(5)
    @DisplayName("Login: unknown user throws AuthException")
    void loginUnknownUser() {
        assertThrows(AuthException.class, () ->
                userRepo.validateLogin("nobody", "pass"));
    }

    // ── MessageService protocol parser ────────────────────────────────────

    @Test
    @Order(6)
    @DisplayName("Parse: PUBLIC line")
    void parsePublic() {
        MessageService.ParsedProtocol p =
                MessageService.parse("PUBLIC:alice:hello world");
        assertNotNull(p);
        assertEquals(MessageType.PUBLIC, p.type);
        assertEquals("alice",       p.sender);
        assertEquals("hello world", p.content);
    }

    @Test
    @Order(7)
    @DisplayName("Parse: PRIVATE line")
    void parsePrivate() {
        MessageService.ParsedProtocol p =
                MessageService.parse("PRIVATE:alice:bob:hey there");
        assertNotNull(p);
        assertEquals(MessageType.PRIVATE, p.type);
        assertEquals("alice",     p.sender);
        assertEquals("bob",       p.recipient);
        assertEquals("hey there", p.content);
    }

    @Test
    @Order(8)
    @DisplayName("Parse: SYSTEM line")
    void parseSystem() {
        MessageService.ParsedProtocol p =
                MessageService.parse("SYSTEM::server notice");
        assertNotNull(p);
        assertEquals(MessageType.SYSTEM, p.type);
        assertEquals("server notice", p.content);
    }

    @Test
    @Order(9)
    @DisplayName("Parse: malformed line returns null")
    void parseMalformed() {
        assertNull(MessageService.parse("garbage"));
        assertNull(MessageService.parse(null));
        assertNull(MessageService.parse(""));
    }

    // ── Message.toProtocolString round-trip ───────────────────────────────

    @Test
    @Order(10)
    @DisplayName("toProtocolString: PUBLIC round-trip")
    void protocolRoundTrip() throws Exception {
        User alice = userRepo.findByUsername("alice");
        assertNotNull(alice);

        Message m = new Message(alice, "hi everyone", MessageType.PUBLIC);
        String  proto = m.toProtocolString();

        assertTrue(proto.startsWith("PUBLIC:alice:"));
        assertTrue(proto.endsWith("hi everyone"));
    }
}

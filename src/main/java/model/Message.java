package model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Message extends AbstractEntity {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("HH:mm");

    private final User          sender;
    private final String        content;
    private final LocalDateTime timestamp;
    private final MessageType   type;
    private       String        recipient;   // non-null for PRIVATE only


    public Message(User sender, String content, MessageType type) {
        this.sender    = sender;
        this.content   = content;
        this.timestamp = LocalDateTime.now();
        this.type      = type;
    }

    public Message(int id, User sender, String content,
                   LocalDateTime timestamp, MessageType type) {
        super(id);
        this.sender    = sender;
        this.content   = content;
        this.timestamp = timestamp;
        this.type      = type;
    }



    public User          getSender()    { return sender; }
    public String        getContent()   { return content; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public MessageType   getType()      { return type; }
    public String        getRecipient() { return recipient; }
    public void          setRecipient(String r) { this.recipient = r; }


    public String toProtocolString() {
        String s = (sender != null) ? sender.getUsername() : "";
        switch (type) {
            case PUBLIC:  return "PUBLIC:"  + s + ":" + content;
            case PRIVATE: return "PRIVATE:" + s + ":" + recipient + ":" + content;
            default:      return "SYSTEM::" + content;
        }
    }

    @Override
    public String toString() {
        String s = (sender != null) ? sender.getUsername() : "SYSTEM";
        return "[" + timestamp.format(FMT) + "] " + s + ": " + content;
    }
}

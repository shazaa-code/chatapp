package service;

import exception.DatabaseException;
import model.Message;
import model.MessageType;
import repository.MessageRepository;
import repository.UserRepository;

import java.util.List;

public class MessageService {

    private final UserRepository    userRepo;
    private final MessageRepository msgRepo;

    public MessageService(UserRepository userRepo, MessageRepository msgRepo) {
        this.userRepo = userRepo;
        this.msgRepo  = msgRepo;
    }

    public void save(Message message) throws DatabaseException {
        msgRepo.save(message);
    }


    public List<Message> loadHistory() throws DatabaseException {
        return msgRepo.getHistory(50);
    }

    public List<Message> loadDMHistory(String a, String b) throws DatabaseException {
        return msgRepo.getDMHistory(a, b);
    }

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

    public static class ParsedProtocol {
        public MessageType type;
        public String      sender;
        public String      recipient;
        public String      content;
    }
}

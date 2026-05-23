package model;

/** Wire-protocol message categories. */
public enum MessageType {
    PUBLIC,   // broadcast to all clients
    PRIVATE,  // routed to one named recipient
    SYSTEM    // server-generated notice (join / leave)
}

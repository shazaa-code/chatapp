-- JavaFX Chat Application — SQLite Schema
-- The application auto-creates both tables on first run.
-- Run this script manually if you want to pre-initialise the database.

CREATE TABLE IF NOT EXISTS users (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    username      TEXT    NOT NULL UNIQUE,
    password_hash TEXT    NOT NULL
);

CREATE TABLE IF NOT EXISTS messages (
    id        INTEGER PRIMARY KEY AUTOINCREMENT,
    sender_id INTEGER NOT NULL,
    content   TEXT    NOT NULL,
    timestamp TEXT    NOT NULL,
    type      TEXT    NOT NULL CHECK(type IN ('PUBLIC','PRIVATE','SYSTEM')),
    recipient TEXT,
    FOREIGN KEY(sender_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_msg_type      ON messages(type);
CREATE INDEX IF NOT EXISTS idx_msg_timestamp ON messages(timestamp);

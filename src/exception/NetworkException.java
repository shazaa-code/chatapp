package exception;

/** Thrown when a TCP socket operation fails (connect, read, write). */
public class NetworkException extends Exception {
    public NetworkException(String message)                  { super(message); }
    public NetworkException(String message, Throwable cause) { super(message, cause); }
}

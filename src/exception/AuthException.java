package exception;

/** Thrown when login credentials are invalid or a username is already taken. */
public class AuthException extends Exception {
    public AuthException(String message)                  { super(message); }
    public AuthException(String message, Throwable cause) { super(message, cause); }
}

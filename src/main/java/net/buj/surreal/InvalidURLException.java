package net.buj.surreal;

public class InvalidURLException extends Exception {
    public InvalidURLException(String message) {
        super(message);
    }

    public InvalidURLException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidURLException(Throwable cause) {
        super(cause);
    }
}

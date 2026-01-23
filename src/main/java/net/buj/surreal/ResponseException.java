package net.buj.surreal;

public class ResponseException extends Exception {
    public ResponseException(String message) {
        super(message);
    }

    public ResponseException(Exception cause) {
        super(cause);
    }
}

package net.buj.surreal;

/**
 * An error reported by SurrealDB or by an event handler.
 */
public class ResponseException extends Exception {
    /**
     * Construct {@link ResponseException}.
     *
     * @param message Error message.
     */
    public ResponseException(String message) {
        super(message);
    }

    /**
     * Construct {@link ResponseException}.
     *
     * @param cause An error that occured when processing SurrealDB response.
     */
    public ResponseException(Exception cause) {
        super(cause);
    }
}

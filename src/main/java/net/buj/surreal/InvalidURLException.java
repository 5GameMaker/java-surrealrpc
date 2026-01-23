package net.buj.surreal;

/**
 * SurrealRPC URL is not valid.
 * <p>
 * See {@link SurrealURL}.
 */
public class InvalidURLException extends Exception {
    /**
     * Construct {@link InvalidURLException}.
     * 
     * @param message A message.
     */
    public InvalidURLException(String message) {
        super(message);
    }

    /**
     * Construct {@link InvalidURLException}.
     * 
     * @param message A message.
     * @param cause   Cause of the error.
     */
    public InvalidURLException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Construct {@link InvalidURLException}.
     * 
     * @param cause Cause of the error.
     */
    public InvalidURLException(Throwable cause) {
        super(cause);
    }
}

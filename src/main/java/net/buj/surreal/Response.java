package net.buj.surreal;

import mjson.Json;

/**
 * A response to a query.
 * <p>
 * May be a failed response, make sure to use {@link Response#isOk()} to check.
 */
public class Response {
    /**
     * Response status.
     * <p>
     * Contains {@code OK} if the execution was successful.
     */
    public final String status;
    /**
     * The amount of time it took to execute a statement.
     */
    public final String timeString;
    /**
     * Execution result.
     * <p>
     * If the query has failed, contains the error as string.
     */
    public final Json result;

    Response(String status, String timeString, Json result) {
        this.status = status;
        this.timeString = timeString;
        this.result = result;
    }

    /**
     * Check if the query has succeeded.
     *
     * @return {@code true} if query has succeeded, {@code false} otherwise.
     */
    public boolean isOk() {
        return status.equals("OK");
    }

    /**
     * Fail if the query has failed.
     *
     * @throws ResponseException If an error has occured.
     *
     * @return Itself.
     */
    public Response ok() throws ResponseException {
        if (!isOk())
            throw new ResponseException(result.isString() ? result.asString() : status);
        return this;
    }
}

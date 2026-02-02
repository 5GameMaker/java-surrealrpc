package net.buj.surreal;

import mjson.Json;

/**
 * A live response.
 * <p>
 * See <a href="https://surrealdb.com/docs/surrealql/statements/live">LIVE
 * SELECT</a>.
 */
public class LiveResponse {
    /**
     * Live query ID.
     * <p>
     * Must be checked before accessing anything else, unless you've used
     * {@link Driver#onLive(String, EventCallback)}.
     */
    public final String id;

    /**
     * A driver that created this live query.
     */
    public final Driver driver;

    /**
     * An action as reported by SurrealDB.
     */
    public final String action;

    /**
     * Data as reported by SurrealDB.
     */
    public final Json data;

    LiveResponse(String id, Driver driver, String action, Json data) {
        this.id = id;
        this.driver = driver;
        this.action = action;
        this.data = data;
    }

    /**
     * Check if IDs match.
     * <p>
     * This is a convenience method cuz Java is annoying.
     * 
     * @param id The ID to check against.
     * @return {@code true} if IDs match, {@code false} otherwise.
     */
    public boolean id(String id) {
        return this.id.equals(id);
    }
}

package net.buj.surreal;

import mjson.Json;

/**
 * A SurrealDB query.
 */
public class Query {
    String sql;
    Json params = Json.object();

    /**
     * Construct a new {@link Query}
     *
     * @param sql SurrealQL query. Use {@code $varname} for referencing variables.
     */
    public Query(String sql) {
        this.sql = sql;
    }

    /**
     * Set a parameter.
     *
     * @param param A parameter to be set.
     * @param value A value for the parameter.
     *
     * @return Itself.
     */
    public Query x(String param, Object value) {
        params.set(param, value);
        return this;
    }
}

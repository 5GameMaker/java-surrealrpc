package net.buj.surreal;

import mjson.Json;

public class Query {
    String sql;
    Json params = Json.object();

    public Query(String sql) {
        this.sql = sql;
    }

    public Query x(String param, Object value) {
        params.set(param, value);
        return this;
    }
}

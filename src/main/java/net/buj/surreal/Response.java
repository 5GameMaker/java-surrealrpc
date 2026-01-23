package net.buj.surreal;

import mjson.Json;

public class Response {
    public final String status;
    public final String timeString;
    public final Json result;

    Response(String status, String timeString, Json result) {
        this.status = status;
        this.timeString = timeString;
        this.result = result;
    }

    public boolean isOk() {
        return status.equals("OK");
    }

    public Response ok() throws ResponseException {
        if (!isOk())
            throw new ResponseException(result.isString() ? result.asString() : status);
        return this;
    }
}

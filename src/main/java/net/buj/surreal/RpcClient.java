package net.buj.surreal;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import mjson.Json;

class RpcClient extends WebSocketClient {
    private List<EventCallback<Json>> callbacks = new ArrayList<>();

    public void request(String method, EventCallback<Json> callback, Object... params) {
        int i = 0;
        for (; i < callbacks.size(); i++)
            if (callbacks.get(i) == null) {
                callbacks.set(i, callback);
                break;
            }
        if (i == callbacks.size())
            callbacks.add(callback);

        Json payload = Json.object();
        payload.set("id", i);
        payload.set("method", method);
        payload.set("jsonrpc", "2.0");
        Json payloadParams = Json.array();
        for (Object param : params) {
            payloadParams.add(param);
        }
        payload.set("params", payloadParams);
        System.err.println(">> " + payload.toString());
        send(payload.toString());
    }

    public RpcClient(URI serverUri) {
        super(serverUri);
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
    }

    @Override
    public void onMessage(String message) {
        System.err.println("<< " + message);
        Json json = Json.read(message);

        int id = json.at("id").asInteger();
        EventCallback<Json> callback = callbacks.get(id);
        callbacks.set(id, null);

        if (json.has("error")) {
            callback.fail(new ResponseException(json.at("error").at("message").asString()));
        } else {
            try {
                callback.run(json.at("result"));
            } catch (Exception t) {
                callback.fail(t);
            }
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
    }

    @Override
    public void onError(Exception ex) {
        for (EventCallback<Json> callback : callbacks) {
            if (callback != null)
                callback.fail(new ResponseException(ex));
        }
        callbacks.clear();
    }
}

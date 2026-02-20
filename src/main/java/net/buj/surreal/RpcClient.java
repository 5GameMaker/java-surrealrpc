package net.buj.surreal;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import mjson.Json;

class RpcClient extends WebSocketClient {
    private List<EventCallback<Json>> callbacks = new ArrayList<>();
    private final Driver driver;

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

        String stringPayload = payload.toString();
        if (driver.debug != null)
            driver.debug.rawMessageSend(stringPayload);
        send(stringPayload);
    }

    private static Map<String, String> headers = new HashMap<>();
    static {
        headers.put("sec-websocket-protocol", "json");
    }

    public RpcClient(Driver driver, URI serverUri) {
        super(serverUri, headers);

        this.driver = driver;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        if (driver.debug != null)
            driver.debug.rpcOpen();
    }

    @Override
    public void onMessage(String message) {
        if (driver.debug != null)
            driver.debug.rawMessageRecv(message);

        Json json = Json.read(message);

        if (!json.has("id")) {
            json = json.at("result");

            LiveResponse response = new LiveResponse(json.at("id").asString(), driver, json.at("action").asString(),
                    json.at("result"));

            synchronized (driver.sLiveListeners) {
                EventCallback<LiveResponse> handle = driver.sLiveListeners.get(response.id);
                if (handle != null) {
                    try {
                        handle.run(response);
                    } catch (Exception e) {
                        handle.fail(e);
                    }
                }
            }

            synchronized (driver.liveListeners) {
                for (EventCallback<LiveResponse> handle : driver.liveListeners) {
                    try {
                        handle.run(response);
                    } catch (Exception e) {
                        handle.fail(e);
                    }
                }
            }

            return;
        }

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

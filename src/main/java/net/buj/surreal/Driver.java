package net.buj.surreal;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import mjson.Json;
import net.buj.surreal.SurrealURL.RootAuthorization;
import net.buj.surreal.SurrealURL.TokenAuthorization;

/**
 */
public class Driver implements AutoCloseable {
    private final List<EventCallback<Object>> connectedListeners = new ArrayList<>();

    public Driver(String url) throws InvalidURLException, URISyntaxException, IOException, InterruptedException {
        this(new SurrealURL(url));
    }

    public Driver(URI uri) throws InvalidURLException, URISyntaxException, IOException, InterruptedException {
        this(new SurrealURL(uri));
    }

    public Driver(SurrealURL url) throws IOException, InterruptedException {
        Objects.requireNonNull(url);

        this.url = url;

        String token;
        if (url.authorization instanceof TokenAuthorization) {
            token = ((TokenAuthorization) url.authorization).token;
        } else if (url.authorization instanceof RootAuthorization) {
            try {
                HttpURLConnection con = (HttpURLConnection) url.loginUri.toURL().openConnection();
                con.setRequestProperty("Accept", "application/json");
                con.setRequestMethod("POST");
                con.setDoInput(true);
                con.setDoOutput(true);

                Json json = Json.object();
                json.set("user", ((RootAuthorization) url.authorization).username);
                json.set("pass", ((RootAuthorization) url.authorization).password);

                con.getOutputStream().write(json.toString().getBytes(StandardCharsets.UTF_8));
                json = Json.read(new String(con.getInputStream().readAllBytes(),
                        StandardCharsets.UTF_8));

                if (json.at("code").asInteger() != 200)
                    throw new IOException(json.at("details").asString());

                token = json.at("token").asString();
            } catch (MalformedURLException oh) {
                throw new RuntimeException("Internal error. This is a bug in SurrealRPC!",
                        oh);
            }
        } else {
            throw new RuntimeException("Unreachable!");
        }

        client = new RpcClient(url.rpcUri);
        client.connectBlocking(5, TimeUnit.SECONDS);

        client.request("authenticate", new EventCallback<Json>() {
            @Override
            public void run(Json value) {
                client.request("use", new EventCallback<Json>() {
                    @Override
                    public void run(Json value) {
                        synchronized (syncObject) {
                            Queue<Object[]> queue = overheadQueue;
                            overheadQueue = null;

                            for (Object[] args : queue)
                                query((Query) args[0], (EventCallback<Response[]>) args[1]);
                        }
                        Object object = new Object();
                        for (EventCallback<Object> listener : connectedListeners)
                            listener.run(object);
                    }

                    @Override
                    public void fail(Exception error) {
                        for (EventCallback<Object> listener : connectedListeners)
                            listener.fail(error);
                    }
                }, url.namespace, url.database);
            }

            @Override
            public void fail(Exception error) {
                for (EventCallback<Object> listener : connectedListeners)
                    listener.fail(error);
            }
        }, token);

        System.out.println(token);
    }

    private final SurrealURL url;
    private final RpcClient client;
    private final Object syncObject = new Object();
    private Queue<Object[]> overheadQueue = new ArrayDeque<>(16);

    public void query(Query query, EventCallback<Response[]> callback) {
        Objects.requireNonNull(query);
        Objects.requireNonNull(callback);

        synchronized (syncObject) {
            if (overheadQueue != null) {
                overheadQueue.add(new Object[] { query, callback });
                return;
            }
        }

        client.request("query", new EventCallback<Json>() {
            @Override
            public void run(Json value) {
                List<Json> list = value.asJsonList();
                Response[] responses = new Response[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    Json json = list.get(i);
                    responses[i] = new Response(json.at("status").asString(), json.at("time").asString(),
                            json.at("result"));
                }
                callback.run(responses);
            }

            @Override
            public void fail(Exception error) {
                callback.fail(error);
            }
        }, query.sql, query.params);
    }

    public void onConnect(EventCallback<Object> callback) {
        Objects.requireNonNull(callback);

        connectedListeners.add(callback);
    }

    @Override
    public void close() throws Exception {
        client.closeBlocking();
    }
}

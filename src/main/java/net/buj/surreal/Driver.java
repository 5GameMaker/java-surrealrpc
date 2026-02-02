package net.buj.surreal;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import mjson.Json;
import net.buj.surreal.SurrealURL.RootAuthorization;
import net.buj.surreal.SurrealURL.TokenAuthorization;

/**
 * SurrealDB driver.
 * <p>
 * SurrealRPC connects to SurrealDB over RPC.
 * <p>
 * SurrealDB driver is mostly asynchronous.
 */
public class Driver implements AutoCloseable {
    private final List<EventCallback<Object>> connectedListeners = new ArrayList<>();

    /**
     * Create a new {@link Driver}.
     *
     * @param url SurrealRPC URL. See {@link SurrealURL}.
     *
     * @throws IOException          If connection to SurrealDB fails.
     * @throws InvalidURLException  If not a valid SurrealRPC URL.
     * @throws URISyntaxException   If not a valid URI.
     * @throws InterruptedException If driver gets interrupted while connecting.
     */
    public Driver(String url) throws InvalidURLException, URISyntaxException, IOException, InterruptedException {
        this(new SurrealURL(url));
    }

    /**
     * Create a new {@link Driver}.
     *
     * @param uri SurrealRPC URL. See {@link SurrealURL}.
     *
     * @throws IOException          If connection to SurrealDB fails.
     * @throws InvalidURLException  If not a valid SurrealRPC URL.
     * @throws URISyntaxException   If not a valid URI.
     * @throws InterruptedException If driver gets interrupted while connecting.
     */
    public Driver(URI uri) throws InvalidURLException, URISyntaxException, IOException, InterruptedException {
        this(new SurrealURL(uri));
    }

    /**
     * Create a new {@link Driver}.
     *
     * @param url SurrealRPC URL.
     *
     * @throws IOException          If connection to SurrealDB fails.
     * @throws InterruptedException If driver gets interrupted while connecting.
     */
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

        client = new RpcClient(this, url.rpcUri);
        client.connectBlocking(5, TimeUnit.SECONDS);

        client.request("authenticate", new EventCallback<Json>() {
            @Override
            public void run(Json value) {
                client.request("use", new EventCallback<Json>() {
                    @Override
                    @SuppressWarnings("unchecked")
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
                    @SuppressWarnings("unchecked")
                    public void fail(Exception error) {
                        for (EventCallback<Object> listener : connectedListeners)
                            listener.fail(error);

                        Queue<Object[]> queue = overheadQueue;
                        overheadQueue = null;

                        for (Object[] args : queue)
                            ((EventCallback<Response[]>) args[1]).fail(error);
                    }
                }, url.namespace, url.database);
            }

            @Override
            @SuppressWarnings("unchecked")
            public void fail(Exception error) {
                for (EventCallback<Object> listener : connectedListeners)
                    listener.fail(error);

                Queue<Object[]> queue = overheadQueue;
                overheadQueue = null;

                for (Object[] args : queue)
                    ((EventCallback<Response[]>) args[1]).fail(error);
            }
        }, token);
    }

    private final SurrealURL url;
    private final RpcClient client;
    private final Object syncObject = new Object();
    private Queue<Object[]> overheadQueue = new ArrayDeque<>(16);

    /**
     * Submit a query to SurrealDB.
     * <p>
     * Query will only execute once driver has successfully connected to
     * the database. If connection fails - all query callbacks will receive
     * errors.
     *
     * @param query    A query to be executed.
     * @param callback A callback to be executed after the execution of the query.
     */
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
                List<Json> list = value.isArray() ? value.asJsonList() : Collections.singletonList(value);
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

    private static <T> T getLast(List<T> list) {
        return list.get(list.size() - 1);
    }

    /**
     * Submit a query with a single output to SurrealDB.
     * <p>
     * Query will only execute once driver has successfully connected to
     * the database. If connection fails - all query callbacks will receive
     * errors.
     * <p>
     * If multiple outputs are returned, only the last one will be forwarded
     * to the handler. Throws a {@link IndexOutOfBoundsException} if there were
     * no outputs.
     *
     * @param query    A query to be executed.
     * @param callback A callback to be executed after the execution of the query.
     */
    public void querySingle(Query query, EventCallback<Response> callback) {
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
                Json json = value.isArray() ? getLast(value.asJsonList()) : value;
                callback.run(new Response(json.at("status").asString(), json.at("time").asString(),
                        json.at("result")));
            }

            @Override
            public void fail(Exception error) {
                callback.fail(error);
            }
        }, query.sql, query.params);
    }

    /**
     * Add a connection callback.
     *
     * @param callback A callback to be executed after the database connection has
     *                 been established.
     */
    public void onConnect(EventCallback<Object> callback) {
        Objects.requireNonNull(callback);

        connectedListeners.add(callback);
    }

    /**
     * Close the client.
     * <p>
     * Closing the client is a blocking operation.
     */
    @Override
    public void close() throws Exception {
        client.closeBlocking();
    }

    /**
     * Handler for debug messages for this driver.
     */
    public DebugHandler debug;
}

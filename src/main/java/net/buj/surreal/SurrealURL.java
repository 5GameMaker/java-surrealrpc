package net.buj.surreal;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * URL parser for SurrealRPC URLs.
 * <p>
 * A valid SurrealRPC URL must have a {@code ws} or {@code wss} scheme,
 * end with 2 segments designated for namespace and path, and contain
 * authorization information.
 */
public class SurrealURL {
    /**
     * An authorization scheme.
     * <p>
     * Should never be implemented by the user.
     */
    public interface Authorization {
    }

    /**
     * Token authorization scheme.
     */
    public static class TokenAuthorization implements Authorization {
        /**
         * Token authorization scheme.
         */
        public final String token;

        TokenAuthorization(String token) {
            this.token = token;
        }
    }

    /**
     * Root authorization scheme.
     */
    public static class RootAuthorization implements Authorization {
        /**
         * Root user username.
         */
        public final String username;
        /**
         * Root user password.
         */
        public final String password;

        RootAuthorization(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }

    /**
     * Construct a new {@link SurrealURL}.
     *
     * @param uri The URI that will be parsed.
     *
     * @throws InvalidURLException If this is not a valid SurrealRPC URL.
     * @throws URISyntaxException  If this is not a valid URI either.
     */
    public SurrealURL(String uri) throws InvalidURLException, URISyntaxException {
        this(new URI(uri));
    }

    /**
     * Construct a new {@link SurrealURL}.
     *
     * @param uri The URI that will be parsed.
     *
     * @throws InvalidURLException If this is not a valid SurrealRPC URL.
     */
    public SurrealURL(URI uri) throws InvalidURLException {
        if (!uri.getScheme().equals("ws") && !uri.getScheme().equals("wss"))
            throw new InvalidURLException("SurrealRPC only supports 'ws' and 'wss' schemes");

        String[] path = uri.getPath().split("/");
        if (path.length < 3)
            throw new InvalidURLException("URI must include namespace and database as last path segments");
        database = path[path.length - 1];
        namespace = path[path.length - 2];

        if (uri.getUserInfo() == null || uri.getUserInfo().isEmpty())
            throw new InvalidURLException("Authorization must be specified");

        String[] auth = uri.getUserInfo().split(":");
        if (auth.length == 1)
            authorization = new TokenAuthorization(auth[0]);
        else if (auth.length == 2)
            authorization = new RootAuthorization(auth[0], auth[1]);
        else
            throw new InvalidURLException("Only root and token schemes are currently supported");

        String loginPath;
        String rpcPath;
        {
            StringBuilder basePath = new StringBuilder();
            String[] rawPath = uri.getRawPath().split("/");
            for (int i = 1; i < rawPath.length - 2; i++)
                basePath.append('/').append(rawPath[i]);

            loginPath = basePath + "/signin";
            rpcPath = basePath + "/rpc";
        }

        try {
            loginUri = new URI((uri.getScheme().equals("ws") ? "http" : "https") + "://" + uri.getHost()
                    + (uri.getPort() == -1 ? "" : ":" + uri.getPort()) + loginPath);
            rpcUri = new URI(uri.getScheme() + "://" + uri.getHost()
                    + (uri.getPort() == -1 ? "" : ":" + uri.getPort()) + rpcPath);
        } catch (URISyntaxException error) {
            throw new RuntimeException("Internal error. This is a bug in SurrealRPC!", error);
        }
    }

    final URI loginUri;
    final URI rpcUri;

    /**
     * SurrealDB namespace.
     */
    public final String namespace;
    /**
     * SurrealDB database.
     */
    public final String database;
    /**
     * Authorization scheme for RPC.
     */
    public final Authorization authorization;
}

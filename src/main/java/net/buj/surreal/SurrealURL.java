package net.buj.surreal;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * URL parser for SurrealRPC URLs.
 */
public class SurrealURL {
    public interface Authorization {
    }

    public static class TokenAuthorization implements Authorization {
        public final String token;

        public TokenAuthorization(String token) {
            this.token = token;
        }
    }

    public static class RootAuthorization implements Authorization {
        public final String username;
        public final String password;

        public RootAuthorization(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }

    public SurrealURL(String uri) throws InvalidURLException, URISyntaxException {
        this(new URI(uri));
    }

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

    public final String namespace;

    public final String getNamespace() {
        return namespace;
    }

    public final String database;

    public final String getDatabase() {
        return database;
    }

    public final Authorization authorization;

    public final Authorization getAuthorization() {
        return authorization;
    }
}

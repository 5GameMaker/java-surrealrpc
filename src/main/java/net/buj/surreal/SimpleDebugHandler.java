package net.buj.surreal;

import java.io.PrintStream;

/**
 * Simple println implementation for {@link DebugHandler}.
 */
public class SimpleDebugHandler extends DebugHandler {
    /**
     * Construct {@link SimpleDebugHandler}.
     *
     * @param stream The print stream that will be used for printing.
     */
    public SimpleDebugHandler(PrintStream stream) {
        this.stream = stream;
    }

    private final PrintStream stream;

    @Override
    public void rpcOpen() {
        stream.println("[SurrealRPC/RPC] Connected");
    }

    @Override
    public void rpcClose() {
        stream.println("[SurrealRPC/RPC] Closed");
    }

    @Override
    public void rpcError(Exception why) {
        stream.println("[SurrealRPC/RPC] An error has occured: " + why.getMessage());
    }

    @Override
    public void rawMessageRecv(String message) {
        stream.println("[SurrealRPC] << " + message);
    }

    @Override
    public void rawMessageSend(String message) {
        stream.println("[SurrealRPC] >> " + message);
    }
}

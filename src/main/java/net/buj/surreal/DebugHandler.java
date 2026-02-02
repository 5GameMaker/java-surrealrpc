package net.buj.surreal;

/**
 * Handler for debug events sent by SurrealRPC.
 */
public abstract class DebugHandler {
    /**
     * Called when RPC opens.
     */
    public void rpcOpen() {
    }

    /**
     * Called when RPC closes.
     */
    public void rpcClose() {
    }

    /**
     * Called when RPC encounters an error.
     *
     * @param why The error.
     */
    public void rpcError(Exception why) {
    }

    /**
     * Called when a raw RPC message was received.
     *
     * @param message The message.
     */
    public void rawMessageRecv(String message) {
    }

    /**
     * Called when a raw RPC message was sent.
     *
     * @param message The message.
     */
    public void rawMessageSend(String message) {
    }
}

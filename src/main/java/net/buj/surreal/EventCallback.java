package net.buj.surreal;

/**
 * General callback interface.
 * <p>
 * Event callbacks are usually removed after use.
 *
 * @param <R> Return value passed to {@link EventCallback#run(Object)}.
 */
public interface EventCallback<R> {
    /**
     * Execute the callback.
     * <p>
     * Usually any errors that arise in this method are propagated
     * to {@link EventCallback#fail(Exception)}.
     *
     * @param value Return value for this event.
     */
    void run(R value);

    /**
     * Process an error.
     *
     * @param error An error that occured.
     */
    void fail(Exception error);
}

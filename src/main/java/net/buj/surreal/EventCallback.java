package net.buj.surreal;

public interface EventCallback<R> {
    void run(R value);

    void fail(Exception error);
}

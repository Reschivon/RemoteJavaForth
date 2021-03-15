package com.example.btclient.Forth;

/**
 *  provider -> BufferedIO -> consumer
 * A custom queue that blocks until either
 * 1. the provider provide the next element or
 * 2. the provider signals "end"
 *
 * Similar to a Unix pipe
 */
public abstract class BufferedIO {
    abstract boolean hasNext();
    abstract String next();
    abstract public void feed(String in);
    abstract void signalEnd();

    boolean done = false;

}

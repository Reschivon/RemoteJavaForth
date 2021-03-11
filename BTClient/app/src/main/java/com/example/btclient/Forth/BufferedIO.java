package com.example.btclient.Forth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 *  provider -> BufferedIO -> consumer
 * A custom queue that blocks until either
 * 1. the provider provide the next element or
 * 2. the provider signals "end"
 *
 * Similar to a Unix pipe
 */
public abstract class BufferedIO implements Interpreter.scan{
//    abstract boolean hasNext();
//    abstract String next();
    abstract public void feed(String in);
    abstract void signalEnd();

    boolean done = false;

    public static class FeedableBufferedIO extends BufferedIO{
        public volatile List<String> nexttoks = new ArrayList<>();

        @Override
        public boolean hasNext() {
            while (nexttoks.size() == 0) {
                if(done)
                    return false;
            }
            return true;
        }

        @Override
        public String next() {
            if(hasNext()){
                return nexttoks.remove(0);
            }
            return null;
        }

        @Override
        public void feed(String line) {
            String rep = line.trim().replaceAll("[^\\S ]+", " ").replaceAll("\\s+"," ");
            nexttoks.addAll(Arrays.asList(rep.split(" ")));
        }

        @Override
        void signalEnd() {
            done = true;
        }
    }

}

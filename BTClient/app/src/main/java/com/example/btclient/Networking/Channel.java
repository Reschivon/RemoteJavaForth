package com.example.btclient.Networking;

import java.io.*;

/**
 * A combination of input and output stream,
 * as well as functions for reading/writing to them
 */
public class Channel {
    private BufferedReader read;
    private OutputStream write;


    public Channel(InputStream instream, OutputStream outstream){
        read = new BufferedReader(new InputStreamReader(instream));
        write = outstream;
    }

    public String read() {
        String ret;
        try {
            ret = read.readLine();
            return ret;
        } catch (IOException e){
            return null;
        }
    }
    public void writeln(String s){
        write(s.concat("\n"));
    }
    public void write(String s){
        if(write == null){
            Bluetooth.log("connect to a server first");
            return;
        }

        try {
            write.write(s.getBytes());
            write.flush();
        } catch (IOException e) {
            e.printStackTrace();
            Bluetooth.log("error on write");
        }

    }

    void close() {
        try {
            write.close();
            read.close();
        } catch (IOException e) {
        }
    }
}

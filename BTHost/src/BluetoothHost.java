import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Scanner;

import javax.bluetooth.*;
import javax.microedition.io.*;

/**
 * Class that implements an SPP Server which accepts single line of
 * message from an SPP client and sends a single line of response to the client.
 */
public class BluetoothHost {

    StreamConnectionNotifier streamConnNotifier;
    BufferedReader bReader;
    PrintWriter pWriter;

    public BluetoothHost() throws BluetoothStateException {
        // display local device address and name
        LocalDevice localDevice = LocalDevice.getLocalDevice();
        System.out.println("Address: "+localDevice.getBluetoothAddress());
        System.out.println("Name: "+localDevice.getFriendlyName());
    }

    volatile boolean active = false;
    Thread listen = new Thread(()->{
        while(true) {
            if(!active)
                continue;
            String lineRead;
            try {
                lineRead = bReader.readLine();
            } catch (IOException e) {
                // connection closed by client
                active = false;
                System.out.println("Connection Closed");
                continue;
            }
            if(lineRead == null){
                // connection closed by client
                active = false;
                System.out.println("Connection Closed");
                continue;
            }
            System.out.println("<Client> " + lineRead);
        }
    });

    Scanner scan = new Scanner(System.in);
    Thread send = new Thread(()->{
        while (true){
            String input = scan.nextLine();
            if(active)
               send(input);
        }
    });

    // http://www.java2s.com/Code/Jar/b/Downloadbluecove211jar.htm
    private void startServer() throws IOException {
        //Create a UUID for SPP
        UUID uuid = new UUID("1101", true);
        //Create the service url
        String connectionString = "btspp://localhost:" + uuid + ";name=Sample SPP Server";

        //open server url
        streamConnNotifier = (StreamConnectionNotifier)
                Connector.open(connectionString);

        listen.start();
        send.start();

    }

    private void deviceSession() throws IOException{
        //Wait for client connection
        System.out.println("\nWaiting for clients to connect");
        StreamConnection connection=streamConnNotifier.acceptAndOpen();

        RemoteDevice dev = RemoteDevice.getRemoteDevice(connection);
        System.out.println("Remote device address: "+dev.getBluetoothAddress());
        System.out.println("Remote device name: "+dev.getFriendlyName(true));

        //send response to spp client
        OutputStream outStream = connection.openOutputStream();
        pWriter = new PrintWriter(
                new OutputStreamWriter(outStream));

        //read string from spp client
        InputStream inStream = connection.openInputStream();
        bReader = new BufferedReader(
                new InputStreamReader(inStream));

        active = true;

        send("Hello from the Host");

        while (active) {
            Thread.onSpinWait();
        }

        pWriter.close();
    }

    private void send(String s){
        if(!active){
            System.out.println("Connect to a host first");
            return;
        }
        pWriter.write(s + "\n");
        pWriter.flush();
    }


    public static void main(String[] args) throws IOException {

        BluetoothHost sampleSPPServer = new BluetoothHost();
        sampleSPPServer.startServer();

        while (true){
            sampleSPPServer.deviceSession();
        }

        // sampleSPPServer.streamConnNotifier.close();

    }
}
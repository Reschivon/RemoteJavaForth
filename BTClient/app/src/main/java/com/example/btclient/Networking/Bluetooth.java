package com.example.btclient.Networking;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.widget.TextView;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class Bluetooth {
    Activity context;
    TextView out;
    private static final int REQUEST_ENABLE_BT = 1;

    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    public Channel currentChannel;

    // Insert your server's MAC address
    private String address;

    // Well known SPP UUID
    private UUID UUID;

    Thread await_message = new Thread(() -> {
        while (true){
            // read string from spp client
            String in = currentChannel.read();
            if (in == null)
                break;

            display(in);
        }

        currentChannel.close();
    });


    public Bluetooth(Activity context, String MAC, String uuid, TextView out){
        this.context = context;
        this.address = MAC;
        this.UUID = UUID.fromString(uuid);

        // Check for Bluetooth support and then check to make sure it is turned on
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        CheckBTState();

        this.out = out;
    }

    public void onResume() {
        log("\n...Resumed...\n...Attempting client connect...");

        // Set up a pointer to the remote node using its address.
        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        // A MAC address, which we got above.
        // A Service ID or UUID.  In this case we are using the UUID for SPP.
        try {
            btSocket = device.createRfcommSocketToServiceRecord(UUID);
        } catch (IOException e) {
            AlertBox("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");
            return;
        }

        // Discovery is resource intensive.  Make sure it isn't going on
        // when you attempt to connect and pass your message.
        btAdapter.cancelDiscovery();

        // Establish the connection.  This will block until it connects.
        try {
            btSocket.connect();
            log("\n...Connection established and data link opened...");
        } catch (IOException e) {
            try {
                btSocket.close();
            } catch (IOException e2) {
                AlertBox("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
            }
            return;
        }

        try {
            currentChannel = new Channel(btSocket.getInputStream(), btSocket.getOutputStream());
        } catch (IOException e) {
            AlertBox("Fatal Error", "In onResume() and output stream creation failed:" + e.getMessage() + ".");
            return;
        }

        //await_message.start();
    }

    public void sendln(Object message){
        if (currentChannel == null) {
            log("You need to connect to server first");
            return;
        }
        currentChannel.writeln(message.toString());
    }

    public void send(String message) {
        if (currentChannel == null) {
            log("You need to connect to server first");
            return;
        }
        currentChannel.write(message);
    }

    private void CheckBTState() {

        // Emulator doesn't support Bluetooth and will return null
        if(btAdapter == null) {
            AlertBox("Fatal Error", "Bluetooth Not supported. Aborting.");
            return;
        }

        if (btAdapter.isEnabled()) {
            log("\n...Bluetooth is enabled...");
        } else {
            //Prompt user to turn on Bluetooth
            Intent enableBtIntent = new Intent(btAdapter.ACTION_REQUEST_ENABLE);
            context.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

    }

    public void onPause() {

        log("\n...Paused...");

        currentChannel.close();

        try {
            btSocket.close();
        } catch (IOException e2) {
            AlertBox("Fatal Error", "In onPause() and failed to close socket." + e2.getMessage() + ".");
        }
    }

    public void AlertBox(String title, String message){
        new AlertDialog.Builder(context)
                .setTitle( title )
                .setMessage( message + " Press OK to exit." )
                .setPositiveButton("OK", (arg0, arg1)->context.finish())
                .show();
    }

    public static void log(String s){
        Log.d("Util Bluetooth", s);
    }
    private void display(String s){
        context.runOnUiThread(() -> out.append(s.concat("\n")));
    }
}

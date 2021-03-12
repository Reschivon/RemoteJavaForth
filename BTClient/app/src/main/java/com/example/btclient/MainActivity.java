package com.example.btclient;

import android.app.Activity;
import android.os.Bundle;
import com.example.btclient.Forth.Interpreter;
import com.example.btclient.Networking.BluetoothClient;
import com.example.bttest.R;

public class MainActivity extends Activity {

  BluetoothClient bluetoothClient;
  Interpreter interpreter;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);

    interpreter = new Interpreter(
            message -> bluetoothClient.send(message),
            new qwer());

    bluetoothClient = new BluetoothClient(
            this, "C8:21:58:6A:A3:A0",
            "00001101-0000-1000-8000-00805F9B34FB",
            findViewById(R.id.out),
            interpreter::feed);
  }

  @Override
  public void onResume() {
    super.onResume();
    bluetoothClient.startHostSession();

    int def = interpreter.new_thread(null); //default thread
    interpreter.set_user_thread(def);

  }

  @Override
  public void onPause() {
    super.onPause();
    bluetoothClient.endHostSession();
  }

  class qwer {
    public void saySomething() {
      bluetoothClient.send("Hi Hi Hi\n");
    }
    public void saySomethingElse(int i) {
      bluetoothClient.send("Hi " + i + "\n");
    }
  }

}
package com.example.btclient;
 
import android.widget.TextView;
import com.example.bttest.R;
import android.app.Activity;
import android.os.Bundle;
import com.example.btclient.Forth.*;
import com.example.btclient.Networking.*;

import java.io.InputStream;

public class MainActivity extends Activity {

  Bluetooth bluetooth;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);

    bluetooth = new Bluetooth(
            this, "C8:21:58:6A:A3:A0",
            "00001101-0000-1000-8000-00805F9B34FB",
            (TextView) findViewById(R.id.out));

    Interpreter.bluetooth = bluetooth;
    Interpreter.setNativeRoot(this);
  }
 
  @Override
  public void onStart() {
    super.onStart();
  }
 
  @Override
  public void onResume() {
    super.onResume();

    bluetooth.onResume();

    bluetooth.sendln("ur mum gaey");

    Interpreter.begin();
  }
 
  @Override
  public void onPause() {
    super.onPause();

    bluetooth.onPause();
  }
 
  @Override
  public void onStop() {
    super.onStop();
  }
 
  @Override
  public void onDestroy() {
    super.onDestroy();
  }

}
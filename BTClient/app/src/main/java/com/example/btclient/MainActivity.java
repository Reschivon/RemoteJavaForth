package com.example.btclient;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
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
		
		interpreter = new Interpreter(                        // create a Forth interpreter
				bluetoothClient::send,                        // send interpreter's output over the bluetooth connection
				new qwer());                                  // object entry point for Forth code to native
		
		TextView local_debug_output
				= findViewById(R.id.out);                     // Textview for showing debug output on Android screen
		
		bluetoothClient = new BluetoothClient(                // create a Bluetooth connection
				s -> runOnUiThread(
						() -> local_debug_output.append(s)),  // handle debug output from the Interpreter
				this,                                  // instance of Android's activity needed to check BT availability
				//      change if not on Android
				"C8:21:58:6A:A3:A0",                   // host's MAC address (*must* be changed)
				"00001101-0000-1000-8000-00805F9B34FB",  // BT UUID, the same as the one in host code
				interpreter::feed);                           // send host messages into the interpreter
	}
	
	@Override
	public void onResume() {
		super.onResume();
		bluetoothClient.startHostSession();           // Connect to Host. Errors will be sent over debug output
		
		int def = interpreter.new_thread(null); // launch thread, equivalent of launching inner interpreter
		interpreter.set_user_thread(def);             // set the user shell to this thread
		
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
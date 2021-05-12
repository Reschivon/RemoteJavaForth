package com.example.btclient;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import com.example.btclient.Forth.Interpreter;
import com.example.btclient.Forth.ReplGraphics.GraphicsCore;
import com.example.btclient.Networking.BluetoothClient;
import com.example.bttest.R;

import java.util.Scanner;

public class MainActivity extends Activity {
	
	BluetoothClient bluetoothClient;
	Interpreter interpreter;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		interpreter = new Interpreter(                        // create a Forth interpreter
				bluetoothClient::send,                        // send interpreter's output over the bluetooth connection
				new dog());                                   // object entry point for Forth code to native
		
		TextView local_debug_output
				= findViewById(R.id.out);                     // Textview for showing debug output on Android screen
		
		bluetoothClient = new BluetoothClient(                // create a Bluetooth connection
				s -> runOnUiThread(
						() -> local_debug_output.append(s)),  // handle debug output from the Interpreter
				this,                                  // instance of Android's activity needed to check BT availability
															  // change if not on Android
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
	
	public static void main(String[] args) {
		GraphicsCore graphics = new GraphicsCore();
		
		Interpreter interpreter = new Interpreter(
				string -> {
					// escape sequence for graphics modules
					if(string.charAt(0) == '\\') {
						graphics.feed(string.substring(1));
					}else{
						System.out.print(string);
					}
				},
				new dog()
		);
		
		Scanner scanner = new Scanner(System.in);
		new Thread(() -> {
			while(true) {
				interpreter.feed(scanner.nextLine());
			}
		}).start();
		
		int def = interpreter.new_thread(null); //default thread
		interpreter.set_user_thread(def);
		
		
		
		
		
		
		
		new dog();
	}
	
	public static class dog{
		public int age = 9;
		public int fur_darkness = 42;
		
		public dog(){}
		public dog(int age){};
		public dog(int age, int fur_darkness){};
		public dog(dog mate){};
		
		
		public int human_years(){
			return age * 7;
		}
		
		public dog offspring(dog mate){
			dog offspring = new dog();
			offspring.fur_darkness
					= (mate.fur_darkness + fur_darkness) / 2;
			offspring.age = 1;
			
			return offspring;
		}
	}
}
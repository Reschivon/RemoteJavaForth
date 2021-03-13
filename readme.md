# Remote Android Forth REPL 
```
PC user shell --- BT Host     <-----remote BT connection--->     Bluetooth Client --- Java Forth Interpreter
```

This library allows the user to communicate with a Forth system running on a remote machine. Through this Forth system, the user can also control Java code running on the remote machine.

## Integrating into your code
On the remote machine:
```
public class MainActivity extends Activity {

  BluetoothClient bluetoothClient;
  Interpreter interpreter;

  /* Your initialization function */
    interpreter = new Interpreter(                    // create a Forth interpreter
            message -> bluetoothClient.send(message), // send interpreter's output over the bluetooth connection
            new Object);                              // object entry point for Forth code to native

    TextView local_debug_output
          = findViewById(R.id.out);                   // Textview for showing debug output on Android screen

    bluetoothClient = new BluetoothClient(            // create a Bluetooth connection
            s -> runOnUiThread(
                  ()->local_debug_output.append(s)),  // handle debug output from the Interpreter
            this,                                     // instance of Android's activity needed to check BT availability
                                                      //      change if not on Android
            "C8:21:58:6A:A3:A0",                      // host's MAC address (*must* be changed)
            "00001101-0000-1000-8000-00805F9B34FB",   // BT UUID, the same as the one in host code
            interpreter::feed);                       // send host messages into the interpreter
            
  /*        */

  /* Begin conenction and shell */
    super.onResume();                       // Android-specific stuff, not relevant to Forth or BT
    bluetoothClient.startHostSession();     // Connect to Host. Errors will be sent over debug output

    int def = interpreter.new_thread(null); // launch thread, equivalent of launching inner interpreter
    interpreter.set_user_thread(def);       // set the user shell to this thread
    
  /*        */
```

Run the Host code on your PC and

Features
- No modification to existing Java codebase
- Forth interpreter supports compilation, branching, xts, etc. but is built with Java speed in mind
- Multi tasking using native Java threads

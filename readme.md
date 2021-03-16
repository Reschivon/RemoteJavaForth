# Remote Android Forth REPL 

This library allows the user to communicate with a Forth system running on a remote machine. There is good support for interop between Forth and existing Java

[![Vidya](http://img.youtube.com/vi/o8DEKkxpQ-8/0.jpg)](http://www.youtube.com/watch?v=o8DEKkxpQ-8 "Video Title")

## Motivation

The need to speed up the development process of FIRST Tech Challenge robotics code. Deploying a single edit requires 20s of build time, manual download thru a cable, repositioning the robot, and restarting the OpMode. However with Forth this can be reduced to nil. (Note: This repo is not dependent on my other robotics code)

- Wirelessly send Forth commands to interpreter running on robot (no touch!)
- Interactively interop with compiled Java
- No modification to existing Java codebase
- Forth interpreter supports compilation, branching, xts, etc. but is built with Java speed in mind
- Multi tasking using native Java threads

```
PC user shell <---> BT Host   <----remote BT connection--->   Bluetooth Client <---> Java Forth Interpreter <---> Exisitng Java Code
```

## Integrating into your code

On the remote client machine:
Ensure that that MAC address specified in the remote client code below is the MAC address for you host

MainActivity.java
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

1. Pair the host and client via bluetooth
2. Then run the host code on your PC and the client code on your remote machine (MainActivity.java is a suggestion for using the client files. It is trivial to modify MainActicity.java for non-Android platforms)
3. You should see the Host reporting that a connection has been made (terminal only for the time being)
5. You may need to find an OSX version of the Bluecove library for macs running the host code

## Java Interpreter
I have emulated enough of a bare-metal Forth system in Java that most regular compiling words are available (working on DOES>). However, as the intention is to conveniently work in a Java environment, this interpreter takes advantage of Java utilities, ex. Threads, HashMaps, and Java-defined primitive routines. To avoid needless performance lost, the inner interpreter does not use NEXT or DOCOL.

Here is the structure of one word in memory

    +-------------------+-----------+----------------- - - - - +------------+------------- - - - -
    | POINTER TO        | LENGTH OF | NAME CHARACTERS          | IMMEDIATE? | ADDRESSES OF 
    | PREVIOUS WORD	    | NAME      |     	                   |            | INSTRUCTIONS
    +--- Integer 32 ----+- Integer -+- n Integers as - - - - - +- Integer --+------------- - - - -
                               ^         Unicode Points
                     next pointer points here

The memory is a Java 32-bit integer array, which means that pointers point to indicies within this array rather than absolute memory locations. 
Java objects are handled by pointers to negative addresses on the stack. Relevant primitives know how to convert negative addresses into objects; the user can conceptually pretend that objects are kept on the stack like any other stack data.

Use `native` to push on stack the native Java object defined in the Java code above

The `->` word pops a Java object from stack and pulls the next token from the input stream. 
a. If the token matches a field of the Java object and the field is type `Integer` then the field value will be pushed to stack. 
b. If no field is found but the token matches a method of the Java object, then the method is invoked. Integer parameters are pulled sequentially from the stack; Object parameters are pulled likewise (negative addresses representing Java objects are magically converted to objects). Then, if there is a return value for the method of type Integer the value is pushed to stack. If the return is type Object then an object is pushed to stack.

The functionality of `->` is very intutive, please don't be scared by the above. Let's do an example
My Java native object is of this class:
```
class dog{
  int age = 9;
  int fur_darkness = 42;
  
  int human_years(){
    return age * 7;
  }
  
  dog offspring(dog mate){
    dog offspring = new dog();
    offspring.fur_darkness
      = (mate.fur_darkness + fur_darkness) / 2;
    offspring.age = 1;
    
    return offspring;
  }
}
```
Now I'm going to put the value of `age` on stack

`native -> age`


See! easy! Now we have 42 on stack


Let's set `age`

`7 native set age`


If we were to print the age in human years:

`native -> human_years .` then 49 would be shown


Let's create a new dog object

`new dog`


Now we have a dog of `age` 9 and `fur_darkness` 42 on stack

I'm going to set its darkness to 50

`50 dup set fur_darkness`


Let's get a newborn dog via the `offspring` method

`native -> offspring`

This takes the recently created dog object from stack and passes it as a parameter. We get a new dog returned and pushed to stack


Let's check it's `fur_darkness`

`-> fur_darkness .`

We get 46, which is the average of the first two dogs, as expected

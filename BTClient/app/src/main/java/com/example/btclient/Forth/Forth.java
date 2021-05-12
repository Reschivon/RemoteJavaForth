package com.example.btclient.Forth;


import com.example.btclient.Forth.ReplGraphics.GraphicsCore;

import java.util.Scanner;

public class Forth {
    public Forth(Object entryPoint){
        GraphicsCore graphics = new GraphicsCore();

        // make new interpreter and handle interpreter stdout
        Interpreter interpreter = new Interpreter(
                string -> {
                    // escape sequence for graphics modules
                    if(string.charAt(0) == '\\')
                        graphics.feed(string.substring(1));
                    else
                        System.out.print(string);
                },
                entryPoint
        );

        // set interpreter input to stdin
        Scanner scanner = new Scanner(System.in);
        new Thread(() -> {
            while(true)
                interpreter.feed(scanner.nextLine());
        }).start();

        // set the user facing task
        int def = interpreter.new_thread(null);
        interpreter.set_user_thread(def);
    }
}

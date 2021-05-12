package com.example.btclient.Forth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class State {
	Interpreter origin;
	
	// state of interpreter
	MutableBoolean immediate = new MutableBoolean(true);
	
	// native java objects, extension of memory (get with negative addresses on stack)
	List<Object> objects = new ArrayList<>();
	
	// data stack
	Interpreter.Stack stack = new Interpreter.Stack();
	
	// call stack for nested execution
	Interpreter.Stack call_stack = new Interpreter.Stack();
	
	// input
	public FeedableBufferedIO input;
	
	String name;
	int id;
	
	private List<Integer> memory;
	private HashMap<Integer, ConsumerWithException<State>> primitives;
	
	public State(Interpreter i, String name, int id){
		this.name = name;
		this.id = id;
		this.input = new FeedableBufferedIO();
		
		origin = i;
		memory = origin.memory;
		primitives = origin.primitives;
		
		call_stack.add(origin.ENTRY_POINT);
		
	}
	
	boolean repl_running = false;
	synchronized public void stop(){
		repl_running = false;
	}
	
	void repl(int action) {
		repl_running = true;
		//DEBUG = true;
		call_stack.add(origin.ENTRY_POINT);
		
		// deal with initial action
		if(action != -1) {
			if (exec_primitive(action)) {
			
			} else {
				call_stack.add(action + memory.get(action));
				call_stack.incrementLast();
			}
		}
		
		while (repl_running) {
			int word_address = memory.get(call_stack.last());
			
			// primitive or forth word?
			if(exec_primitive(word_address)) {
				//System.out.println("exec prim at " + origin.primitive_names.get(word_address));
				
			}else{
				// execute forth word
				//System.out.println("exec forth" + origin.read_string(word_address));
				call_stack.add(word_address + memory.get(word_address));
			}
			
			//advance code pointer
			call_stack.incrementLast();
		}
		
		origin.threads.remove(id);
	}
	
	public void interpret() {
		// usually EOF when reading from file
		if(!input.hasNext()) {
			// origin.outputln("\n....end of file", id);
			repl_running = false;
			return;
		}
		
		// get next token from input
		String next_word = input.next();
		
		//is carriage return
		if(next_word.equals("\n")) {
			System.out.print("OK " + "STK:" + stack.size() + " " + (immediate.get()?"":"CMPL") + ">");
			return;
		}
		
		// TODO why am I getting zero-length tokens
		if(next_word.equals("")) {
			return;
		}
		
		// if it's a number, then deal with the number and skip to next
		try{
			int val = Integer.parseInt(next_word);
			if(immediate.get()) {
				stack.add(val);
			}else{
				memory.add(origin.search_word("lit"));
				memory.add(val);
			}
			return;
		}catch(NumberFormatException ignored){}
		
		// it is a token, not a number
		// find address of word identified by token
		int address = origin.search_word(next_word);
		
		// does address correspond to existing word?
		if(origin.search_word(next_word) == -1) {
			// word not found
			origin.outputln("word " + next_word + " not found", id);
		}else{
			// word found
			// is compiled or executed?
			boolean a = false,b = false;
			
			if(immediate.get()) {
				a = true;
				//System.out.println("\timmediate state");
			}
			if(memory.get(origin.addressToFlag(address)) == 1) {
				b = true;
				//System.out.println("\tword is flagged immediate");
			}
			
			// execute
			if(a || b){
				if(exec_primitive(address)){
				
				}else {
					//System.out.println("\tinterpret places " + origin.read_string(address) + " on stack " + address);
					call_stack.add(address + memory.get(address));
				}
			}else{
				// compile word
				//System.out.println("\tcompile " + origin.primitive_names.get(address));
				memory.add(address);
			}
		}
	}
	//return true if primitive exists
	private boolean exec_primitive(int address){
		if(primitives.containsKey(address)){
			try {
				primitives.get(address).accept(this);
			}catch (Exception e) {
				origin.outputln("Exception ", id);
				e.printStackTrace();
			}
			return true;
		} else {
			return false;
		}
	}
	
	void addObject(Object o){
		objects.add(o);
		stack.add(-objects.size());
	}
	Object getObject(){
		return objects.get( (-stack.pop())-1 );
	}
	
}

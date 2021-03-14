package com.example.btclient.Forth;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
	
	List<Integer> memory;
	HashMap<Integer, Consumer<State>> primitives;
	
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
	
	/**
	 *
	 * @param action return value of search_word
	 */
	void repl(int action) {
		repl_running = true;
		//DEBUG = true;
		call_stack.add(origin.ENTRY_POINT);
		
		// deal with initial action
		if(action != -1) {
			if (primitives.containsKey(action)) {
				primitives.get(action).accept(this);
			} else {
				call_stack.add(action + memory.get(action));
				call_stack.incrementLast();
			}
		}
		/* All instructions must be stored in memory, even the
		 * uncompiled immediate mode stuff. ENTRY_POINT is a pointer
		 * to an integer of reserved space for instructions
		 * executed directly from the input stream. At ENTRY_POINT + 1
		 * is a return which will clear the pointer to ENTRY_POINT
		 * after the instruction there executes
		 */

//       for(int i=0;i<30;i++){
		while (repl_running) {
			int word_address = memory.get(call_stack.last());
			
			// primitive or forth word?
			if(primitives.containsKey(word_address)) {
				// execute primitive
				try {
					//System.out.println("exec " + origin.primitive_names.get(word_address));
					// immediate and primitive
					primitives.get(word_address).accept(this);
					
				}catch (Exception e) {
					origin.outputln("Uncaught Exception ", id);
					e.printStackTrace();
				}
				
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
			origin.outputln("....end of file", id);
			repl_running = false;
			return;
		}
		
		// get next token from input
		String next_Word = input.next();
		
		
		// if it's a number, then deal with the number and skip to next
		try{
			int val = Integer.parseInt(next_Word);
			if(immediate.get()) {
				stack.add(val);
			}else{
				memory.add(origin.search_word("literal"));
				memory.add(val);
			}
			return;
		}catch(NumberFormatException ignored){}
		
		// it is a token, not a number
		// find address of word identified by token
		int address = origin.search_word(next_Word);
		
		// does address correspond to existing word?
		if(origin.search_word(next_Word) == -1) {
			// word not found
			origin.outputln("word " + next_Word + " not found", id);
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
				//System.out.println("\tinterpret places " + origin.read_string(address) + " on stack");
				if(primitives.containsKey(address)){
					primitives.get(address).accept(this);
				}else {
					call_stack.add(address + memory.get(address));
				}
			}else{
				// compile word
				//System.out.println("\tcompile " + origin.primitive_names.get(address));
				memory.add(address);
			}
		}
	}
	
	void dotOperator() throws InvocationTargetException, IllegalAccessException {
		// the calling object
		Object actor = getObject();
		// the name of the object's field or method
		String fieldOrClass = input.next();
		
		// get actual type of attribute
		Object attribute = ReflectionMachine.getByString(actor, fieldOrClass);
		
		if (attribute == null) {
			origin.outputln("field or class " + fieldOrClass + " not found as attribute", id);
		}
		
		// call a method and push return to stack
		else if(attribute instanceof Method){
			Method themethod = ((Method)attribute);
			
			// get parameters
			Object[] params = new Object[themethod.getParameterTypes().length];
			
			for(int i=0; i<params.length;i++){
				int stackElem = stack.pop();
				// stack has object address or integer?
				params[i] = (stackElem < 0)? objects.get(-stackElem-1):stackElem;
			}
			// invoke
			Object returnval = themethod.invoke(actor, params);
			
			// manage return as object or integer
			if(returnval instanceof Integer){
				stack.add((int)returnval);
			} else {// is object
				objects.add(returnval);
				stack.add(-objects.size());
			}
			
		}
		// get the value of field
		else if(attribute instanceof Field) {
			Field thefield = ((Field)attribute);
			if (thefield.getType() == int.class || thefield.getType() == Integer.class) {
				stack.add(thefield.getInt(actor));
			}
			else {//is object
				addObject(thefield.get(actor));
			}
		}
	}
	
	
	
	void addObject(Object o){
		objects.add(o);
		stack.add(-objects.size());
	}
	Object getObject(){
		return objects.get( (-stack.pop())-1 );
	}


    /*String read_string(int address, List<Integer> memory)
    {
        if(memory.get(address)-1<0)
            origin.outputln("string length invalid: "+(memory.get(address)-1));

        // read length of string
        byte[] str = new byte[memory.get(address)-1];
        int offset = address+1;

        // read codepoints
        for(int i = 0; i<str.length; i++){
            str[i] = (byte)(int)memory.get(offset + i);
        }

        // convert codepoints to string object
        return new String(str);
    }*/
	
}

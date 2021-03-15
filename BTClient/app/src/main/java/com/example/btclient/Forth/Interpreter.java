package com.example.btclient.Forth;

import com.example.btclient.ReplGraphics.GraphicsCore;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;


public class Interpreter {
	
	// address of first pointer in the linked list that comprises the dictionary
	int HERE = -1;
	// address of initial opcode, or main() for c programmers.
	// will be populated later
	int ENTRY_POINT;
	
	// memory. Double array
	List<Integer> memory = new ArrayList<>();
	
	// starting point for native java methods and fields
	Object nativeRoot = null;
	public void setNativeRoot(Object o){nativeRoot = o;}
	// link up names of primitives to their java code
	HashMap<Integer, Consumer<State>> primitives = new HashMap<>();
	
	HashMap<Integer, String> primitive_names = new HashMap<>();
	
	private State current_thread = null;
	private int thread_counter = 0;
	HashMap<Integer, State> threads = new HashMap<>();
	
	static class deer{
		public int h = 69;
		public void g(){
			System.out.println("solp");
		}
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
				new deer()
		);
		
		Scanner scanner = new Scanner(System.in);
		new Thread(() -> {
			while(true) {
				interpreter.feed(scanner.nextLine());
			}
		}).start();
		
		int def = interpreter.new_thread(null); //default thread
		interpreter.set_user_thread(def);
	}
	
	public Interpreter(outputListener outputL, Object root) {
		setOutputListener(outputL);
		setNativeRoot(root);
		
		init();
	}
	
	public void init(){
		// initialization thread
		int init_thread_id = new_thread(null);
		State init_thread = threads.get(init_thread_id);
		
		init_thread.input.feed(startup);
		init_thread.input.setautoTerminate(true);
		
		outputln("Startup file found and run", init_thread.id);
	}
	
	public int new_thread(String action){
		State s;
		int id = thread_counter;
		
		//default thread
		if(action == null){
			s = new State(this, "default", id);
			new Thread(() -> s.repl(-1)).start();
		}else{
			s = new State(this, action, id);
			s.input.setautoTerminate(true);
			new Thread(() -> s.repl(search_word(action))).start();
		}
		threads.put(thread_counter, s);
		
		return thread_counter++;
	}
	
	public void set_user_thread(int num){
		current_thread = threads.get(num);
	}
	
	public void feed(String s){
		current_thread.input.feed(s);
	}
	// output
	public interface outputListener{
		void outputInvoked(String message);
	}
	private outputListener outputListener;
	public void setOutputListener(outputListener o){
		outputListener = o;
	}
	
	public final boolean SHOW_OUTPUT_FROM_OTHER_THREADS = true;
	void output(Object s, int id){
		if(current_thread != null && id != current_thread.id) {
			if(SHOW_OUTPUT_FROM_OTHER_THREADS)
				outputListener.outputInvoked("Thread " + id + "> " + s.toString());
		} else {
			outputListener.outputInvoked(s.toString());
		}
	}
	void outputln(Object s, int id){
		output(s.toString() + "\n", id);
	}
	
	//fuck!
	List<String> string_pool = new ArrayList<>();
	
	{
		declarePrimitive( "new" , state -> {}); /*newOperator();*/
		declarePrimitive( "dodot" , state -> {
			// index of field or class
			String fieldOrClass = string_pool.get(state.stack.pop());
			// the calling object
			Object actor = state.getObject();
			try {
				ReflectionMachine.dot_operator(actor, fieldOrClass, state);
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
			
		});
		declarePrimitive( "->" , state -> {
			if(state.immediate.get()) {
				// the calling object
				Object actor = state.getObject();
				// the name of the object's field or method
				String fieldOrClass = state.input.next_token();
				
				try {
					ReflectionMachine.dot_operator(actor, fieldOrClass, state);
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}
			}else{
				// index of field or class
				memory.add(state.origin.search_word("lit"));
				string_pool.add(state.input.next());
				memory.add(string_pool.size()-1);
				memory.add(state.origin.search_word("dodot"));
			}
		}, true);
		declarePrimitive( "fields", state -> ReflectionMachine.fields(state.getObject(), state));
		declarePrimitive( "methods" , state -> ReflectionMachine.methods(state.getObject(), state));
		declarePrimitive( "objects" , state -> {
			for (Object tok : state.objects)
				output(tok.getClass().getName() + " ", state.id);
			outputln("<-", state.id);
		});
		declarePrimitive( "native" , state -> state.addObject(nativeRoot));
		declarePrimitive( ".S" , state -> {
			for (int tok : state.stack)
				output(tok + " ", state.id);
			outputln("<-", state.id);
		});
		declarePrimitive( "words" , state -> list_words(state.id));
		declarePrimitive( "see" , state -> show_mem(state.input.next_token(), state.id));
		declarePrimitive( "here" , state -> state.stack.add(memory.size()));
		declarePrimitive( "latest" , state -> state.stack.add(HERE));
		declarePrimitive( "." , state -> outputln(state.stack.pop(), state.id));
		declarePrimitive( "exit" , state -> state.call_stack.remove(state.call_stack.size() - 1), true);
		declarePrimitive( "word" , state -> state.stack.add(search_word(state.input.next_token())));
		declarePrimitive( "," , state -> memory.add(state.stack.pop()));
		declarePrimitive( "[" , state -> state.immediate.set(true), true);
		declarePrimitive( "]" , state -> state.immediate.set(false));
		declarePrimitive( "lit" , state -> {
			state.call_stack.incrementLast();
			state.stack.add(memory.get(state.call_stack.last()));
		}, true);
		declarePrimitive( "@" , state -> state.stack.add(memory.get(state.stack.pop())));
		declarePrimitive( "!" , state -> { //value, address <-- top of stack
			int address = state.stack.pop();
			if (address == memory.size()) memory.add(state.stack.pop());
			else memory.set(address, state.stack.pop());
		});
		declarePrimitive( "+" , state -> state.stack.add(state.stack.pop() + state.stack.pop()));
		declarePrimitive( "-" , state -> state.stack.add(-state.stack.pop() + state.stack.pop()));
		declarePrimitive( "*" , state -> state.stack.add(state.stack.pop() * state.stack.pop()));
		declarePrimitive( "=" , state -> state.stack.add(state.stack.pop() == state.stack.pop() ? 1 : 0));
		declarePrimitive( "not" , state -> state.stack.add(state.stack.pop() == 0 ? 1 : 0));
		declarePrimitive( "and" , state -> state.stack.add(state.stack.pop() & state.stack.pop()));
		declarePrimitive( "or" , state -> state.stack.add(state.stack.pop() | state.stack.pop()));
		declarePrimitive( "xor" , state -> state.stack.add(state.stack.pop() ^ state.stack.pop()));
		declarePrimitive( "swap" , state -> {
			int p = state.stack.pop();
			state.stack.add(state.stack.size() - 1, p);
		});
		declarePrimitive( "over" , state -> state.stack.add(state.stack.get(state.stack.size()-2)));
		declarePrimitive( "dup" , state -> state.stack.add(state.stack.last()));
		declarePrimitive( "drop" , state -> state.stack.remove(state.stack.size() - 1));
		
		declarePrimitive( "token>memory" , state -> write_string(state.input.next_token(), memory.size()));
		declarePrimitive( "print-string" , state -> outputln(read_string(state.stack.pop()), state.id));
		declarePrimitive( "create" , state -> {
			memory.add(HERE);
			HERE = memory.size();
		});
		declarePrimitive( "branch" , state -> //advance pointer by instruction at current pointer position
				state.call_stack.set(state.call_stack.size() - 1, state.call_stack.last() + memory.get(state.call_stack.last() + 1)));
		declarePrimitive( "branch?", state ->  {
			if (state.stack.last() == 0) //advance pointer by instruction at current pointer position
				state.call_stack.setLast(state.call_stack.last() + memory.get(state.call_stack.last() + 1));
			else //jump over the offset by 1
				state.call_stack.incrementLast();
			state.stack.pop();
		});
		declarePrimitive("interpret", State::interpret);
		declarePrimitive("greet", state -> outputln("\\xyrplot 4 4 0.5", state.id));
		declarePrimitive("stop", State::stop);
		declarePrimitive("async", state -> state.stack.add(new_thread(state.input.next_token())));
		declarePrimitive("threads", state ->{
			ArrayList<Integer> sortedKeys = new ArrayList<>(threads.keySet());
			Collections.sort(sortedKeys);
			for(int i : sortedKeys) {
				outputln(i + ": " + threads.get(i).name
						+ (threads.get(i).id == current_thread.id?" <-":""), state.id);
			}
		});
		declarePrimitive("switch-thread", state ->{
			// so the current user thread will never end
			// while the user is using it
			if(!current_thread.name.equals("default"))
				current_thread.input.setautoTerminate(true);
			current_thread = threads.get(state.stack.pop());
			current_thread.input.setautoTerminate(false);
		});
		declarePrimitive("stop-thread", state -> threads.get(state.stack.pop()).stop());
		//TODO make less crude
		declarePrimitive("wait", state ->{
			try {
				Thread.sleep(state.stack.pop());
			} catch (InterruptedException e) {
				outputln("Task interrupted", state.id);
			}
		});
		
		create(":");
		memory.add(search_word("create"));
		memory.add(search_word("token>memory"));
		memory.add(search_word("lit"));
		memory.add(0);
		memory.add(search_word(","));
		memory.add(search_word("]"));
		memory.add(search_word("exit"));
		
		create(";");
		memory.add(search_word("["));
		memory.add(search_word("lit"));
		memory.add(search_word("exit"));
		memory.add(search_word(","));
		memory.add(search_word("exit"));
		set_immediate();
		
		ENTRY_POINT = memory.size();
		memory.add(search_word("interpret"));
		memory.add(search_word("branch"));
		memory.add(-2);
	}

    /*void newOperator() throws IllegalAccessException, InvocationTargetException, InstantiationException {
        String classname = input.next();
        Constructor[] constructors;
            try {
        constructors = Class.forName(classname).getConstructors();
            } catch (ClassNotFoundException e){
                outputln(classname + " was not found. Fully qualified names required");
        return;
            }

        outputln("Constructor params: ");
        Class[] o = constructors[0].getParameterTypes();
        for(Class i:o){
            output(i + " ");
        }

        Object[] params = new Object[constructors[0].getParameterTypes().length];
        for(int i=0; i<params.length; i++){
            params[i] = stack.pop();
        }
        addObject(constructors[0].newInstance(params));
    }
*/
	// (caller object '' name of attribute -- return value or address of returned object)
	
	/**
	 * See the other definition of write_string
	 * This defaults to writing to memory array
	 */
	void write_string(String name, int address)
	{
		write_string(name, address, memory);
	}
	
	/**
	 * Writes a java object string to the provided array as
	 * Unicode Points. First Integer is chars in String + 1
	 * @param name The Java Object string
	 * @param address The index of the first integer written
	 * @param list The array to be written to
	 */
	void write_string(String name, int address, List<Integer> list) {
		byte[] b = name.getBytes();
		// length integer
		list.add(address, b.length+1);
		
		// write Unicode Codepoints
		for(int i = 0; i<b.length; i++){
			list.add(address+i+1, (int)b[i]);
		}
	}
	
	/**
	 * Reads a list of Unicode Points, with the first integer
	 * being the length of the string + 1
	 * @param address address in the mem array of first integer
	 * @return String in java object format
	 */
	String read_string(int address) {
		//if(memory.get(address)-1<0)
		//outputln("string length invalid: "+(memory.get(address)-1));
		
		// read length of string
		byte[] str = new byte[memory.get(address)-1];
		int offset = address+1;
		
		// read codepoints
		for(int i = 0; i<str.length; i++){
			str[i] = (byte)(int)memory.get(offset + i);
		}
		
		// convert codepoints to string object
		return new String(str);
	}
	
	void list_words(int id){
			// walk through dictionary
			int here = HERE;
			while(here != -1){
				String word_name = read_string(here);
				output(word_name + " ", id);
				
				//move to next word in linked list
				here = memory.get(here-1);
			}
			outputln("", id);
	}
	
	/**
	 * Scans the memory and prints each word, in order of
	 * declaration, along with its definition
	 * Ignores other non-word data, like variable values
	 * Should only be used for debugging; assumptions made
	 */
	void show_mem(String target_word_name, int id) {   //make array of word addresses
		List<Integer> pointers = new ArrayList<>();
		
		// read and store word addresses
		int here = HERE;
		while(here != -1){
			pointers.add(here);
			//move to next word in linked list
			here = memory.get(here-1);
		}
		
		for(int i = pointers.size()-1;i>=0;i--)
		{
			int word_address = pointers.get(i);
			
			String word_name = read_string(word_address);
			
			if(!word_name.equals(target_word_name))
				continue;
			
			int immediate = memory.get(word_address + memory.get(word_address));
			
			String setPlainText = "\033[0;0m";
			String setBoldText = "\033[0;1m";
			output(
					String.format(
							"%-25s %d %s ",
							setBoldText + word_name + setPlainText, word_address,
							immediate==1?"immdt":"     "),
					id);
			
			// first opcode of the word
			int instruction_address = addressToOp(word_address);
			
			// hard stop for instructions
			// prevent infinite run-on if there is memory corruption
			int nextop = i==0? memory.size():pointers.get(i-1) - 1;
			
			// iterate through instructions
			for(int j=instruction_address; j<nextop; j++)
			{   // derive name from address
				int mem = memory.get(j);
				String name = read_string(mem);
				
				//stop iterating if return encountered
				if(name.equals("exit"))
					break;
				
				output(name + " ", id);
				
				// special condition if next element is read as lit
				if(name.equals("lit") || name.equals("branch") || name.equals("branch?"))
				{
					j++;
					output(memory.get(j) + " ", id);
				}
			}
			outputln("", id);
			
			return;
		}
		outputln("word not found", id);
		
	}
	
	/**
	 * Interpreter use only, do not confuse with Forth word
	 * Appends a new a word definition stub to memory array (no instructions)
	 */
	void create(String name){
		// add link in linked list to prev word
		memory.add(HERE);
		// update head of linked list
		HERE = memory.size();
		// add name
		write_string(name, memory.size());
		// add immediate flag (default non immediate)
		memory.add(0);
	}
	
	/**
	 * Interpreter use only. Sets the most recently defined word as immediate
	 */
	void set_immediate(){
		memory.set(HERE + memory.get(HERE), 1);
	}
	
	/**
	 * Interpreter use only. Searches through dictionary
	 * for a word that matches the name in the string
	 * @return Address of word, if found. -1 if not found
	 */
	int search_word(String name) {
		int here = HERE;
		while(here != -1)
		{
			String word_name = read_string(here);
			if(name.equals(word_name))
				return here;
			//move 'here' back
			here = memory.get(here-1);
		}
		return -1;
	}
	
	/**
	 * See other definition of declare primitive. Defaults
	 * to non-immediate
	 */
	void declarePrimitive(String name, Consumer<State> r) {
		declarePrimitive(name, r, false);
	}
	
	/**
	 * Word only for interpreter use. Creates primitive word definition stubs in memory
	 * @param name name of word
	 * @param immediate Flags for immediate word
	 */
	void declarePrimitive(String name, Consumer<State> r, boolean immediate) {   // add link in linked list
		create(name);
		
		// register word as primitive
		// allow the relevant java code to be found
		primitives.put(HERE, r);
		primitive_names.put(HERE, name);
		
		if(immediate)
			memory.set(addressToFlag(HERE), 1);
		
	}
	/**
	 * Convert word address to address of its immediate flag
	 */
	public int addressToFlag(int address) {
		return address + memory.get(address);
	}
	/**
	 * Convert word address to address of its first instruction
	 */
	public int addressToOp(int address) {
		return address + memory.get(address) + 1;
	}
	/**
	 * Basically an Integer Arraylist but with convenient methods
	 */
	public static class Stack extends ArrayList<Integer> {
		public int pop(){
			return remove(size()-1);
		}
		
		public int last(){
			return get(size()-1);
		}
		
		public void incrementLast(){
			add(pop() + 1);
		}
		
		public void setLast(int val){
			set(size()-1, val);
		}
	}
	
	
	String startup = "";
	void string_processor(String... s){
		StringBuilder cum = new StringBuilder();
		for(String i : s){
			cum.append(i);
			cum.append(" ");
		}
		startup = cum.toString();
	}
	
	{
		string_processor("",
				": immediate\n",
				"        	latest @\n",
				"        	latest +",
				"        	1 swap",
				"        	!",
				";",
				
				": [compile]",
				"        	word ,",
				"; immediate",
				
				": [word] word ; immediate",
				
				": ll [word] lit [word] lit , , ; immediate",
				
				": ' ll , word , ; immediate",
				
				": postpone",
				"        	[compile] '",
				"        	' , ,",
				"; immediate",
				
				": if",
				"        	' branch? ,",
				"        	here",
				"        	0 ,",
				"; immediate",
				
				": unless",
				"        	postpone not",
				"        	[compile] if",
				"; immediate",
				
				": then",
				"        	dup",
				"        	here swap -",
				"        	swap !",
				"; immediate",
				
				": else",
				"		' branch ,",
				"		here",
				"		0 ,",
				"		swap",
				"		dup",
				"		here swap -",
				"		swap !",
				"; immediate",
				
				": begin",
				"		here",
				"; immediate",
				
				": until",
				"		' branch? ,",
				"		here -",
				"		,",
				"; immediate",
				
				": again",
				"		' branch ,",
				"		here -",
				"		,",
				"; immediate",
				
				": while",
				"		' branch? ,",
				"		here",
				"		0 ,",
				"; immediate",
				
				": repeat",
				"		' branch ,",
				"		swap",
				"		here - ,",
				"		dup",
				"		here swap -",
				"		swap !",
				"; immediate",
				
				": ) ;",
				
				": (",
				"        	[compile] lit [word] ) [ , ]",
				"        	begin",
				"        	    dup word =",
				"        	until",
				"        	drop",
				"; immediate",
				
				
				"( TODO: functionality to nest parentheses )",
				
				": constant ( initial_value '' constant_name -- )",
				"        	create              ( ! up a new word )",
				"        	token>memory",
				"        	0 ,",
				
				"        	postpone lit    ( add lit instruction to variable definition )",
				"        	,           ( append initial value to memory )",
				"        	postpone exit     ( add exit instruction to constant definition )",
				";",
				
				": variable ( initial_value '' variable_name -- )",
				"        	here         ( push memory address to stack )",
				"        	swap",
				"        	here !     ( append top of stack to memory )",
				
				"        	create              ( ! up a new word )",
				"        	token>memory",
				"        	0 ,",
				
				"        	postpone lit    	( add lit instruction to variable definition )",
				"        	,           ( append pointer to memory )",
				"        	postpone exit     ( add exit instruction to variable definition )",
				";",
				
				": iftest if 22 . else 11 . then ;",
				
				": whiletest begin 11 . 1 until ;",
				
				": unlesstest unless 11 . else 22 . then ;",
				
				"0 unlesstest",
				"1 unlesstest",
				
				"0 iftest",
				"1 iftest",
				
				"whiletest",
				
				": trojan-print postpone . ; immediate",
				
				": troy 22 trojan-print 11 . ;",
				
				"troy",
				
				"22 . ( 55 . ) 11 .",
				
				"( commentation! ) ( exciting! )",
				
				"( cannot nest parentheses )",
				
				"( You can enable aggressive error messages with the word 'profanity' )",
				
				"22 constant burgers",
				"burgers .",
				
				"11 variable pies",
				"pies @ .",
				
				"22 pies !",
				"pies @ .",
				
				": go begin 42 . 1500 wait again ;"

//                "async greet",
//                "th@s"
		);
	}
}

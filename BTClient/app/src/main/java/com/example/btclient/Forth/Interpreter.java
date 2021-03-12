package com.example.btclient.Forth;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;


public class Interpreter {
    public boolean DEBUG = false;
    private final String setPlainText = "\033[0;0m";
    private final String setBoldText = "\033[0;1m";
    private boolean profanity;

    // address of first pointer in the linked list that comprises the dictionary
    int HERE = -1;
    // address of initial opcode, or main() for c programmers.
    // will be populated later
    int ENTRY_POINT = -1;
    State state;

    // memory. Double array
    List<Integer> memory = new ArrayList<>();

    // starting point for native java methods and fields
    Object nativeRoot = null;
    public void setNativeRoot(Object o){nativeRoot = o;}
    // link up names of primitives to their java code
    HashMap<Integer, Consumer<State>> primitives = new HashMap<>();

    HashMap<Integer, String> primitive_names = new HashMap<>();
    // output
    public interface outputListener{
        void outputInvoked(String message);
    }
    private outputListener outputListener;
    public void setOutputListener(outputListener o){
        outputListener = o;
    }

    void output(Object s){
        outputListener.outputInvoked(s.toString());
    }
    void outputln(Object s){
        output(s.toString() + "\n");
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

    {
        /*MutableBoolean immediate = state.immediate;
        List<Object> objects = state.objects;
        Stack stack = state.stack;
        Stack call_stack = state.call_stack;*/


        declarePrimitive( "new" , state -> {}); /*newOperator();*/
        declarePrimitive( "/" , state -> {
            try {
                state.dotOperator();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        });
        declarePrimitive( "fields", state -> ReflectionMachine.fields(state.getObject()));
        declarePrimitive( "methods" , state -> ReflectionMachine.methods(state.getObject()));
        declarePrimitive( "seeobj" , state -> {
            for (Object tok : state.objects)
                output(tok.getClass().getName() + " ");
            outputln("<-");
        });
        declarePrimitive( "native" , state -> state.addObject(nativeRoot));
        declarePrimitive( "seestack" , state -> {
            for (int tok : state.stack)
                output(tok + " ");
            outputln("<-");
        });
        declarePrimitive( "seemem" , state -> show_mem());
        declarePrimitive( "seerawmem" , state -> outputln(memory));
        declarePrimitive( "memposition" , state -> state.stack.add(memory.size()));
        declarePrimitive( "here" , state -> state.stack.add(HERE));
        declarePrimitive( "print" , state -> outputln(state.stack.pop()));
        declarePrimitive( "return" , state -> {
            state.call_stack.remove(state.call_stack.size() - 1);
        }, true);
        declarePrimitive( "word" , state -> state.stack.add(search_word(state.input.next())));
        declarePrimitive( "stack>mem" , state -> memory.add(state.stack.pop()));
        declarePrimitive( "[" , state -> state.immediate.set(true), true);
        declarePrimitive( "]" , state -> {
            state.immediate.set(false);

        });
        declarePrimitive( "literal" , state -> {
            state.call_stack.incrementLast();
            state.stack.add(memory.get(state.call_stack.last()));
        }, true);
        declarePrimitive( "read" , state -> state.stack.add(memory.get(state.stack.pop())));
        declarePrimitive( "donothing" , state -> output(""));
        declarePrimitive( "set" , state -> { //value, address <-- top of stack
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
        declarePrimitive( "over" , state -> {
            state.stack.add(state.stack.get(state.stack.size()-2));
        });
        declarePrimitive( "dup" , state -> state.stack.add(state.stack.last()));
        declarePrimitive( "drop" , state -> state.stack.remove(state.stack.size() - 1));

        declarePrimitive( "stringliteral" , state -> write_string(state.input.next(), memory.size()));
        declarePrimitive( "read-string" , state -> outputln(read_string(state.stack.pop())));
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
        declarePrimitive("interpret", state ->
                state.interpret()
        );declarePrimitive("greet", state ->
            System.out.println("HELLO HELLO HELLO HELLO")
        );

                create(":");
        memory.add(search_word("create"));
        memory.add(search_word("stringliteral"));
        memory.add(search_word("literal"));
        memory.add(0);
        memory.add(search_word("stack>mem"));
        memory.add(search_word("]"));
        memory.add(search_word("return"));

        create(";");
        memory.add(search_word("["));
        memory.add(search_word("literal"));
        memory.add(search_word("return"));
        memory.add(search_word("stack>mem"));
        memory.add(search_word("return"));
        set_immediate();

        ENTRY_POINT = memory.size();
        memory.add(search_word("interpret"));
        memory.add(search_word("branch"));
        memory.add(-2);
    }

    public void repl(){
        // set the reserved address to nothing; the program will populate this
        // accordingly as it parses the input stream
        //memory.set(ENTRY_POINT, search_word("donothing"));

        // set the instruction after to return
        //memory.set(ENTRY_POINT+1, search_word("return"));

        // set call stack to execute the reserved instruction address
        state.call_stack.add(ENTRY_POINT);
        state.repl();
    }

    public static void main(String[] args) {
        Interpreter interpreter = new Interpreter(
                System.out::print,
                new Object());

        Scanner scanner = new Scanner(System.in);
        new Thread(() -> {
            while(true) {
                //TODO figure out why these cant be one line
                String feed = scanner.nextLine();
                interpreter.state.input.feed(feed);
            }
        }).start();
        new Thread(() -> interpreter.begin()).start();

    }

    public void begin() {
        // run from terminal input
        state.input = new BufferedIO.FeedableBufferedIO();
        outputln("Commence Interactive Remote REPL");
        repl();
        output("REPL is over");
    }

    public Interpreter(outputListener outputL, Object root){
        setOutputListener(outputL);
        setNativeRoot(root);

        state = new State(this);

        state.input = new BufferedIO.FeedableBufferedIO();
        state.input.feed(startup);
        state.input.signalEnd();

        repl();
        outputln("Startup file found and run");
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
    void write_string(String name, int address, List<Integer> list)
    {
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
    String read_string(int address)
    {
        if(memory.get(address)-1<0)
            outputln("string length invalid: "+(memory.get(address)-1));

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

    /**
     * Scans the memory and prints each word, in order of
     * declaration, along with its definition
     * Ignores other non-word data, like variable values
     * Should only be used for debugging; assumptions made
     */
    void show_mem()
    {   //make array of word addresses
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
            int immediate = memory.get(word_address + memory.get(word_address));

            output(
                    String.format("%-25s %d %s ", setBoldText + word_name + setPlainText, word_address, immediate==1?"immdt":"     "));

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
                if(name.equals("return"))
                    break;

                output(name + " ");

                // special condition if next element is read as literal
                if(name.equals("literal") || name.equals("branch") || name.equals("branch?"))
                {
                    j++;
                    output(memory.get(j) + " ");
                }
            }
            outputln("");
        }

        outputln("");
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

    interface scan{
        boolean hasNext();
        String next();
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
        string_processor(
                ": immediate\n",
                "        	here read\n",
                "        	here +",
                "        	1 swap",
                "        	set",
                ";",

                ": [compile]",
                "        	word stack>mem",
                "; immediate",

                ": words seemem ;",

                ": [word] word ; immediate",

                ": ll [word] literal [word] literal stack>mem stack>mem ; immediate",

                ": token>stack ll stack>mem word stack>mem ; immediate",

                ": postpone",
                "        	[compile] token>stack",
                "        	token>stack stack>mem stack>mem",
                "; immediate",

                ": if",
                "        	token>stack branch? stack>mem",
                "        	memposition",
                "        	0 stack>mem",
                "; immediate",

                ": unless",
                "        	postpone not",
                "        	[compile] if",
                "; immediate",

                ": then",
                "        	dup",
                "        	memposition swap -",
                "        	swap set",
                "; immediate",

                ": else",
                "		token>stack branch stack>mem",
                "		memposition",
                "		0 stack>mem",
                "		swap",
                "		dup",
                "		memposition swap -",
                "		swap set",
                "; immediate",

                ": begin",
                "		memposition",
                "; immediate",

                ": until",
                "		token>stack branch? stack>mem",
                "		memposition -",
                "		stack>mem",
                "; immediate",

                ": again",
                "		token>stack branch stack>mem",
                "		memposition -",
                "		stack>mem",
                "; immediate",

                ": while",
                "		token>stack branch? stack>mem",
                "		memposition",
                "		0 stack>mem",
                "; immediate",

                ": repeat",
                "		token>stack branch stack>mem",
                "		swap",
                "		memposition - stack>mem",
                "		dup",
                "		memposition swap -",
                "		swap set",
                "; immediate",

                ": ) ;",

                ": (",
                "        	[compile] literal [word] ) [ stack>mem ]",
                "        	begin",
                "        	    dup word =",
                "        	until",
                "        	drop",
                "; immediate",


                "( TODO: functionality to nest parentheses )",

                ": constant ( initial_value '' constant_name -- )",
                "        	create              ( set up a new word )",
                "        	stringliteral",
                "        	0 stack>mem",

                "        	postpone literal    ( add literal instruction to variable definition )",
                "        	stack>mem           ( append initial value to memory )",
                "        	postpone return     ( add return instruction to constant definition )",
                ";",

                ": = constant ; ( just thought it made sense )",

                ": variable ( initial_value '' variable_name -- )",
                "        	memposition         ( push memory address to stack )",
                "        	swap",
                "        	memposition set     ( append top of stack to memory )",

                "        	create              ( set up a new word )",
                "        	stringliteral",
                "        	0 stack>mem",

                "        	postpone literal    ( add literal instruction to variable definition )",
                "        	stack>mem           ( append pointer to memory )",
                "        	postpone return     ( add return instruction to variable definition )",
                ";",

                ": iftest if 22 print else 11 print then ;",

                ": whiletest begin 11 print 1 until ;",

                ": unlesstest unless 11 print else 22 print then ;",

                "0 unlesstest",
                "1 unlesstest",

                "0 iftest",
                "1 iftest",

                "whiletest",

                ": trojan-print postpone print ; immediate",

                ": troy 22 trojan-print 11 print ;",

                "troy",

                "22 print ( 55 print ) 11 print",

                "( commentation! ) ( exciting! )",

                "( cannot nest parentheses )",

                "( You can enable aggressive error messages with the word 'profanity' )",

                "22 constant burgers",
                "burgers print",

                "11 variable pies",
                "pies read print",

                "22 pies set",
                "pies read print"
        );
    }
}

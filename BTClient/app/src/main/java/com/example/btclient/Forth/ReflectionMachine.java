package com.example.btclient.Forth;

import dalvik.system.DexFile;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ReflectionMachine {
	static Field getField(Object actor, String name){
		Field f = null;
		try {
			f = actor.getClass().getField(name);
		} catch (NoSuchFieldException ignored) {
		}
		
		return f;
	}
	static Object getByString(Object actor, String name){
		Field f = getField(actor, name);
		
		Method m = null;
		for(Method p:actor.getClass().getMethods()){
			if(p.getName().equals(name)){
				m = p;
				break;
			}
		}
		
		if(m != null) return m;
		return f;
	}
	
	public static void methods(Object target, State o){
		Method[] l = target.getClass().getMethods();
		for(Method f:l){
			o.origin.outputln("Method: " + f.toString(), o.id);
		}
		o.origin.outputln("", o.id);
	}
	
	public static void fields(Object target, State o){
		Field[] l = target.getClass().getFields();
		if(l.length == 0){
			o.origin.outputln("No fields found", o.id);
			return;
		}
		for(Field f:l){
			o.origin.outputln(f.getName(), o.id);
		}
		o.origin.outputln("", o.id);
	}
	
	public static void set_operator(Object actor, String field, int val, State state){ try{
		// get actual type of attribute
		Field theField = ReflectionMachine.getField(actor, field);
		
		if (theField == null) {
			state.origin.outputln("field or class " + field + " not found as field", state.id);
		}
		
		if(val >= 0){ // It's an integer
			theField.setInt(actor, val);
		}else{ // It's an object
			theField.set(actor, state.objects.get((-val)-1));
		}
		
	}catch(Exception e){
		state.origin.outputln("Something went wrong, see if the new value is right type", state.id);
	}}
	
	public static void dot_operator(Object actor, String fieldOrClass, State state){ try{
		// get actual type of attribute
		Object attribute = ReflectionMachine.getByString(actor, fieldOrClass);
		
		if (attribute == null) {
			state.origin.outputln("field or class " + fieldOrClass + " not found as attribute", state.id);
		}
		
		// call a method and push return to stack
		else if(attribute instanceof Method){
			Method themethod = ((Method)attribute);
			
			// get parameters
			Object[] params = new Object[themethod.getParameterTypes().length];
			
			for(int i=0; i<params.length;i++){
				int stackElem = state.stack.pop();
				// stack has object address or integer?
				params[i] = (stackElem < 0)? state.objects.get(-stackElem-1):stackElem;
			}
			// invoke
			Object returnval = themethod.invoke(actor, params);
			
			// manage return as object or integer
			if(returnval instanceof Integer){
				state.stack.add((int)returnval);
			} else {// is object
				state.objects.add(returnval);
				state.stack.add(-state.objects.size());
			}
			
		}
		// get the value of field
		else if(attribute instanceof Field) {
			Field thefield = ((Field)attribute);
			if (thefield.getType() == int.class || thefield.getType() == Integer.class) {
				state.stack.add(thefield.getInt(actor));
			}
			else {//is object
				state.addObject(thefield.get(actor));
			}
		}
	}catch(Exception e){
		state.origin.outputln("Something went wrong, you're on your own", state.id);
	}}
	
	public static void new_operator(String classname, State state) throws IllegalAccessException, InvocationTargetException, InstantiationException {
		Constructor[] constructors;
		try {
			constructors = Class.forName(classname).getConstructors();
		} catch (ClassNotFoundException e) {
			state.origin.outputln(classname + " was not found. Fully qualified names required", state.id);
			return;
		}
		
		if (constructors.length != 0) {
			Class[] o = constructors[0].getParameterTypes();
			for (Class i : o)
				state.origin.output(i + " ", state.id);
		}else{
			// no arg constructor
			try {
				state.addObject(Class.forName(classname).newInstance());
				return;
			} catch (ClassNotFoundException ignored){
				// never happens, we checked earlier
			}
		}

        Object[] params = new Object[constructors[0].getParameterTypes().length];
        for(int i=0; i<params.length; i++){
            params[i] = state.stack.pop();
        }
        try {
			state.addObject(constructors[0].newInstance(params));
		} catch (IllegalArgumentException e){
			state.origin.outputln("Creation failed. Expected constructor args: ", state.id);
			if (constructors.length != 0) {
				Class[] o = constructors[0].getParameterTypes();
				for (Class i : o)
					state.origin.output(i + " ", state.id);
			}
		}
    }
	
	public static void classes(State state){
	
	}
	
}

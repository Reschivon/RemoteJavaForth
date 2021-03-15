package com.example.btclient.Forth;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ReflectionMachine {
	static Object getByString(Object actor, String name){
		Field f = null;
		try {
			f = actor.getClass().getField(name);
		} catch (NoSuchFieldException ignored) {
		}
		
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
	
	public static void methods(Object target){
		Method[] l = target.getClass().getMethods();
		for(Method f:l){
			System.out.println("Method: " + f);
		}
		System.out.println();
	}
	
	public static void fields(Object target){
		Field[] l = target.getClass().getFields();
		for(Field f:l){
			System.out.println(f);
		}
		System.out.println();
	}
	
	
	public static void dot_operator(Object actor, String fieldOrClass, State state) throws IllegalAccessException, InvocationTargetException {
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
	}
	
}

package com.example.btclient.Forth;

import java.lang.reflect.Field;
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
	
	public static void methods(Object target, Consumer<String> o){
		Method[] l = target.getClass().getMethods();
		for(Method f:l){
			o.accept("Method: " + f);
		}
		o.accept("");
	}
	
	public static void fields(Object target, Consumer<String> o){
		Field[] l = target.getClass().getFields();
		for(Field f:l){
			o.accept(f.toString());
		}
		o.accept("");
	}
}

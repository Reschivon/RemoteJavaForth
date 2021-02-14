package com.example.btclient.Forth;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ReflectionMachine {
    static Object getByString(Object actor, String name){
        Field f = null;
        try {
            f = actor.getClass().getField(name);
        } catch (NoSuchFieldException e) {
        }

        Method m = null;
        for(Method p:actor.getClass().getMethods()){
            if(p.getName().equals(name)){
                m = p;
                break;
            }
        }

        if(m != null) return m;
        if(f != null) return f;
        return null;
    }

    public static void methods(Object target){
        Method[] l = target.getClass().getMethods();
        for(Method f:l){
            System.out.println(f);
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
}

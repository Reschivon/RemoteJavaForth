package com.example.btclient.Forth;

import java.lang.reflect.InvocationTargetException;

public interface ConsumerWithException<Param> {
	void accept(Param p) throws IllegalAccessException, InstantiationException, InvocationTargetException;
}

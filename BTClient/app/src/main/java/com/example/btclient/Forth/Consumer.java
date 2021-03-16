package com.example.btclient.Forth;

import java.lang.reflect.InvocationTargetException;

public interface Consumer<Param> {
	void accept(Param p);
}

package com.example.btclient.Forth;

public class MutableBoolean {
	boolean b;
	
	public void set(boolean b) {
		this.b = b;
	}
	public boolean get(){
		return b;
	}
	
	public MutableBoolean(boolean b){
		this.b = b;
	}
}

package com.example.btclient.Forth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class FeedableBufferedIO extends BufferedIO {
	private volatile List<String> nexttoks = new ArrayList<>();
	
	// end when next requested and tok buffer is empty
	volatile boolean autoTerminate = false;
	
	@Override
	public boolean hasNext() {
		while (nexttoks.size() == 0) {
			if (done || autoTerminate)
				return false;
		}
		return true;
	}
	
	public void setautoTerminate(boolean b){
		autoTerminate = b;
	}
	
	// exclude the line feeds that could be sent through the input stream
	String next_token(){
		String next = next();
		if(next.equals("\n") || next.equals(""))
			return next_token();
		return next;
	}
	
	@Override
	public String next() {
		if (hasNext()) {
			return nexttoks.remove(0);
		}
		return null;
	}
	
	@Override
	public synchronized void feed(String line) {
		String rep = line.trim().replaceAll("[^\\S ]+", " ").replaceAll("\\s+", " ");
		nexttoks.addAll(Arrays.asList(rep.split(" ")));
		
		// if interactive, then each feed is called after user hits enter
		if(!autoTerminate) {
			nexttoks.add("\n");
		}
	}
	
	@Override
	void signalEnd() {
		done = true;
	}
}

package com.example.btclient.Forth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FeedableBufferedIO extends BufferedIO {
	public volatile List<String> nexttoks = new ArrayList<>();
	
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
	
	@Override
	public String next() {
		if (hasNext()) {
			return nexttoks.remove(0);
		}
		return null;
	}
	
	@Override
	public void feed(String line) {
		String rep = line.trim().replaceAll("[^\\S ]+", " ").replaceAll("\\s+", " ");
		nexttoks.addAll(Arrays.asList(rep.split(" ")));
	}
	
	@Override
	void signalEnd() {
		done = true;
	}
}

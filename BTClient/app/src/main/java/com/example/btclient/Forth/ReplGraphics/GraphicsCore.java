package com.example.btclient.Forth.ReplGraphics;

import com.example.btclient.Forth.ReplGraphics.XYRPlot.ValLabel;

import java.util.HashMap;

public class GraphicsCore {
	HashMap<String, DataHandler> data_handler_objects = new HashMap<>();
	HashMap<String, Supplier<DataHandler>> data_handlers = new HashMap<>();

	public GraphicsCore(){
		data_handlers.put("label", () -> new ValLabel(this));
	}
	
	public void feed (String line){
		String target_component = line.substring(0, line.indexOf(" "));
		String[] target_parameters = line.substring(line.indexOf(" ")).split(" ");
		
		if(!data_handler_objects.containsKey(target_component)){
			data_handler_objects.put(target_component, data_handlers.get(target_component).get());
		}
		data_handler_objects.get(target_component).accept(target_parameters);
		
	}

	public void remove_object(DataHandler d){
		data_handler_objects.remove(d);
	}
}

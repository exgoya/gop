package model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ResultCommon {

	public String name;
	public int value;
	public String timestamp;
	public String tag;

	public ResultCommon(String name, int value, String tag, String timestamp) {
		super();
		this.name = name;
		this.value = value;
		this.timestamp = timestamp;
		this.tag = tag;
	}

	@Override
	public String toString() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String json = gson.toJson(this);
		
		return json;
	}
	
}

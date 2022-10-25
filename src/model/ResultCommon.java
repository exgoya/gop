package model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ResultCommon {

	public String name;
	public int value;
	public String tag;
	public boolean alert;


	public ResultCommon(String name, int value,  String tag, boolean alert) {
		super();
		this.name = name;
		this.value = value;
		this.tag = tag;
		this.alert = alert;
	}


	public ResultCommon() {
		super();
	}


	@Override
	public String toString() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String json = gson.toJson(this);
		
		return json;
	}
	
}

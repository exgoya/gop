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


	public String getName() {
		return name;
	}


	public void setName(String name) {
		this.name = name;
	}


	public int getValue() {
		return value;
	}


	public void setValue(int value) {
		this.value = value;
	}


	public String getTag() {
		return tag;
	}


	public void setTag(String tag) {
		this.tag = tag;
	}


	public boolean isAlert() {
		return alert;
	}


	public void setAlert(boolean alert) {
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

package model;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ResultCommonNoBoolean {

	public String name;
	public int value;
	public String tag;
	public int alert;

	public ResultCommonNoBoolean(String name, int value,  String tag, boolean alert) {
		super();
		this.name = name;
		this.value = value;
		this.tag = tag;
		this.alert = booleanPrimitiveToIntTernary(alert);
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


	public int isAlert() {
		return alert;
	}


	public void setAlert(int alert) {
		this.alert = alert;
	}


	public ResultCommonNoBoolean(ResultCommon[] rc) {
		super();
//		for (int i = 0; i < rc.length; i++) {
//			this.name = rc[i].name;
//			this.value = rc[i].value;
//			this.tag = rc[i].tag;
//			this.alert = booleanPrimitiveToIntTernary(rc[i].alert);
//		}
	}

	public ResultCommonNoBoolean(String name2, int value2, String tag2, int alert2) {
		// TODO Auto-generated constructor stub
	}

	public int booleanPrimitiveToIntTernary(boolean foo) {
	    return (foo) ? 1 : 0;
	}
	
	@Override
	public String toString() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String json = gson.toJson(this);
		
		return json;
	}
	
}

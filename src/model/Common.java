package model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Common {
	public String name;
	public boolean diff;
	public int alertValue;
	public int alertPolicy;
	public String sql;
	public String tag;
	public boolean isOs;

	public Common(String name, boolean diff, int alertValue, int alertPolicy, String sql, String tag, boolean isOs) {
		super();
		this.name = name;
		this.diff = diff;
		this.alertValue = alertValue;
		this.alertPolicy = alertPolicy;
		this.sql = sql;
		this.tag = tag;
		this.isOs = isOs;
	}

	@Override
	public String toString() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String json = gson.toJson(this);

		return json;
	}
}

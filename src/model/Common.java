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
	public boolean sqlIsOs;
	public String alertScript;
	public boolean alertScriptIsOs;

	public Common(String name, boolean diff, int alertValue, int alertPolicy, String sql, String tag, boolean sqlIsOs,String alertScript, boolean alertScriptIsOs) {
		super();
		this.name = name;
		this.diff = diff;
		this.alertValue = alertValue;
		this.alertPolicy = alertPolicy;
		this.sql = sql;
		this.tag = tag;
		this.sqlIsOs = sqlIsOs;
		this.alertScript = alertScript;
		this.alertScriptIsOs = alertScriptIsOs;
	}

	@Override
	public String toString() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String json = gson.toJson(this);

		return json;
	}
}

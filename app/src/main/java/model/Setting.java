package model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Setting {

	public JdbcSource jdbcSource;
	public int timeInterval;
	public boolean consolePrint;
	public int pagesize;
	public FileLog fileLog;
	public Stacker stacker;

	@Override
	public String toString() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String json = gson.toJson(this);

		return json;
	}
}

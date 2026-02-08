package model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Setting {

	public JdbcSource jdbcSource;
	public String source;
	public String configId;
	public int timeInterval;
	public String retention;
	public boolean consolePrint;
	public int pageSize;
	public FileLog fileLog;
	public Boolean printCSV; // ansi or csv
	public Api api;
	public String runtimeConfigPath;

	@Override
	public String toString() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String json = gson.toJson(this);

		return json;
	}
}

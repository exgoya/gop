package model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Config {
	public Host host;
	public Common[] common;

	public Config(Host host, Common[] common) {
		super();
		this.host = host;
		this.common = common;
	}

	@Override
	public String toString() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String json = gson.toJson(this);

		return json;
	}
}
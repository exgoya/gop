package model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Config {
	public Integer schemaVersion;
	public Setting setting;
	public ServerSetting server;
	public Measure[] measure;
	public MeasureV2[] measureV2;
	public SourceConfig[] sources;

	public Config(Setting setting, Measure[] measure) {
		super();
		this.setting = setting;
		this.measure = measure;
	}

	@Override
	public String toString() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String json = gson.toJson(this);

		return json;
	}
}

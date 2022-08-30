

import java.util.Arrays;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Config {
    public Host host;
    public Goldilocks[] goldilocks;
	public Config(Host host, Goldilocks[] common) {
		super();
		this.host = host;
		this.goldilocks = goldilocks;
	}
	@Override
	public String toString() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String json = gson.toJson(this);
		
		return json;	}
}
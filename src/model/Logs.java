package model;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Logs {
	public ResultCommon[] logs;

	public Logs(ResultCommon[] logs) {
		super();
		this.logs = logs;
	}
	
	@Override
	public String toString() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String json = gson.toJson(this);
		
		return json;	
	}
	
}

package model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Data {
	public String time;
	public ResultCommon rc[];

	public Data(String time, ResultCommon[] resultArr) {
		super();
		this.time = time;
		this.rc = resultArr;
	}

	@Override
	public String toString() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String json = gson.toJson(this);

		return json;
	}
}

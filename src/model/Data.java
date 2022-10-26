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

	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}

	public ResultCommon[] getRc() {
		return rc;
	}

	public void setRc(ResultCommon[] rc) {
		this.rc = rc;
	}

	@Override
	public String toString() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String json = gson.toJson(this);

		return json;
	}
}

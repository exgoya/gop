package model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class DataNoBoolean {
	public String time;
	public ResultCommonNoBoolean rc[];

	public DataNoBoolean(String time, ResultCommonNoBoolean[] resultArr) {
		super();
		this.time = time;
		this.rc = resultArr;
	}


//	public DataNoBoolean newInstance(DataNoBoolean data) {
//		return new DataNoBoolean(data);
//	}

	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}

	public ResultCommonNoBoolean[] getRc() {
		return rc;
	}

	public void setRc(ResultCommonNoBoolean[] rc) {
		this.rc = rc;
	}

	@Override
	public String toString() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String json = gson.toJson(this);

		return json;
	}

}

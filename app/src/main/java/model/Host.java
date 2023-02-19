package model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Host {

	public String name;
	public String ip;
	public int port;
	public String user;
	public String password;
	public int timeInterval;
	public String logPath;
	public boolean print;
	public int pagesize;

	public Host(String name, String ip, int port, String user, String password) {
		super();
		this.name = name;
		this.ip = ip;
		this.port = port;
		this.user = user;
		this.password = password;
	}

	public Host(String name, String ip, int port, String user, String password, int timeInterval, String logPath,
		boolean print,int pagesize) {
		super();
		this.name = name;
		this.ip = ip;
		this.port = port;
		this.user = user;
		this.password = password;
		this.timeInterval = timeInterval;
		this.logPath = logPath;
		this.print = print;
		this.pagesize = pagesize;
	}

	@Override
	public String toString() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String json = gson.toJson(this);

		return json;
	}
}

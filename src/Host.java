import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Host {

	public String name;
	public String ip;
	public int port;
	public String user;
	public String password;
	
	
	public Host(String name, String ip, int port, String user, String password) {
		super();
		this.name = name;
		this.ip = ip;
		this.port = port;
		this.user = user;
		this.password = password;
	}

	@Override
	public String toString() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String json = gson.toJson(this);
		
		return json;	
	}
}

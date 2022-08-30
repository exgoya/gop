import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Host {

	public String name;
	public String ip;
	public String port;
	
	public Host(String name, String ip, String port) {
		super();
		this.name = name;
		this.ip = ip;
		this.port = port;
	}

	@Override
	public String toString() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String json = gson.toJson(this);
		
		return json;	
	}
}

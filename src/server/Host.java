package server;

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
		return "host [name=" + name + ", ip=" + ip + ", port=" + port + "]";
	}
	
}

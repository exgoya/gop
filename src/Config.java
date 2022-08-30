

import java.util.Arrays;

public class Config {
    public Host host;
    public Common[] common;
	public Config(Host host, Common[] common) {
		super();
		this.host = host;
		this.common = common;
	}
	@Override
	public String toString() {
		return "Config [host=" + host + ", common=" + Arrays.toString(common) + "]";
	}
}
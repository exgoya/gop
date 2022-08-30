

import java.util.Arrays;

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
		return "Config [host=" + host + ", goldilocks=" + Arrays.toString(goldilocks) + "]";
	}
}
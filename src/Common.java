

public class Common {
	public String name;
	public boolean enable;
	public boolean display;
	public boolean diff;
	public int threshold_column;
	public int threshold_max;
	
	public Common(String name, boolean enable, boolean display, boolean diff, int threshold_column, int threshold_max) {
		super();
		this.name = name;
		this.enable = enable;
		this.display = display;
		this.diff = diff;
		this.threshold_column = threshold_column;
		this.threshold_max = threshold_max;
	}

	@Override
	public String toString() {
		return "Common [name=" + name + ", enable=" + enable + ", display=" + display + ", diff=" + diff
				+ ", threshold_column=" + threshold_column + ", threshold_max=" + threshold_max + "]";
	}

}

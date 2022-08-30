

public class Goldilocks {
	public String name;
	public boolean enable;
	public boolean display;
	public boolean diff;
	public int alertColumn;
	public int alertValue;
	public int alertPolicy;
	public String sql;
	
	public Goldilocks(String name, boolean enable, boolean display, boolean diff, int alertColumn, int alertValue,
			int alertPolicy, String sql) {
		super();
		this.name = name;
		this.enable = enable;
		this.display = display;
		this.diff = diff;
		this.alertColumn = alertColumn;
		this.alertValue = alertValue;
		this.alertPolicy = alertPolicy;
		this.sql = sql;
	}

	@Override
	public String toString() {
		return "Goldilocks [name=" + name + ", enable=" + enable + ", display=" + display + ", diff=" + diff
				+ ", alertColumn=" + alertColumn + ", alertValue=" + alertValue + ", alertPolicy=" + alertPolicy
				+ ", sql=" + sql + "]";
	}
}

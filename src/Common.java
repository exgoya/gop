import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Common {
	public String name;
	public boolean enable;
	public boolean display;
	public boolean diff;
	public int alertColumn;
	public int alertValue;
	public int alertPolicy;
	public String sql;
	
	public Common(String name, boolean enable, boolean display, boolean diff, int alertColumn, int alertValue,
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
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String json = gson.toJson(this);
		
		return json;
	}
}

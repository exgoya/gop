

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.sql.Timestamp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Gop {
	public static void main(String[] args) throws FileNotFoundException, IOException, SQLException {

		//read config
		try(FileInputStream fis = new FileInputStream("resource/config.json");
		    InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
		    BufferedReader reader = new BufferedReader(isr))
		{
		    Gson gson = new GsonBuilder().create();
		    Config config = gson.fromJson(reader, Config.class);
		    
		    //db 
		    Db db = new Db(config);
		    db.getCommonQuery();
		    
		    //all print
		    //System.out.println(config.toString());
		    //select print
		    //System.out.println(config.host.name);
		    //System.out.println(config.common[1].name);
		}
		//test 

	    String json = new GsonBuilder()
	               .setDateFormat("yyyy-MM-dd hh:mm:ss.SSS")
	               .create()
	               .toJson(new Timestamp(System.currentTimeMillis()));

	    System.out.println(json);
	}
}
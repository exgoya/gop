
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Gop {
	public static void main(String[] args) throws FileNotFoundException, IOException, SQLException, InterruptedException {

	    boolean first = true;
	    
		//read config
		try(FileInputStream fis = new FileInputStream("resource/config.json");
		    InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
		    BufferedReader reader = new BufferedReader(isr))
		{
		    Gson gson = new GsonBuilder().setPrettyPrinting().create();
		    Config config = gson.fromJson(reader, Config.class);
		    
		    //db 
		    Db db = new Db(config);
		    Connection con = db.createConnection();
		    while(true) {
		    	
		    	ResultCommon[] rc = db.getCommonQuery(con);
		    	
		    	//write output file (json)
		    	FileWriter fw = new FileWriter("resource/out.json");
		    	gson.toJson(rc, fw);
		        fw.flush();        
		        fw.close();
		        
		    	//print console (table)
		    	String[] column = new String[rc.length];
		    	int[] row = new int[rc.length];
		    	
		    	for (int i = 0; i < rc.length; i++) {
		    		column[i] = rc[i].name;
		    		row[i] = rc[i].value;
		    	//System.out.println(rc[i].toString());
				}
		    	
		    	if(first == true) {
	    			System.out.format("%30s", "time");
		    		for (int i = 0; i < row.length; i++) {
		    			System.out.format("%15s", column[i]);
					}
		    		System.out.format("%n");
		    		first = false;
		    	}
		    	
	    		System.out.format("%30s",getTime() );
		    	for (int i = 0; i < row.length; i++) {
		    		System.out.format("%15d", row[i]);
				}
		    	System.out.format("%n");
		    	Thread.sleep(1000); 
		    }
		    
		    //all print
		    //System.out.println(config.toString());
		    //select print
		    //System.out.println(config.host.name);
		    //System.out.println(config.common[1].name);
		}
		//test 
	}
	
	private static String getTime() {
		// TODO Auto-generated method stub
	    String sysTimeStamp = new GsonBuilder()
	               .setDateFormat("yyyy-MM-dd hh:mm:ss")
	               .create()
	               .toJson(new Timestamp(System.currentTimeMillis()));
		return sysTimeStamp;
	}
}


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Properties;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import sunje.goldilocks.jdbc.GoldilocksDriver;

public class Gop {
	public static Properties properties = new Properties();
	public static Connection con_goldi = null;
	public static void main(String[] args) throws FileNotFoundException, IOException {

		//db information from properties
		FileReader resources = null;
		resources = new FileReader("resource/db.properties");
		properties.load(resources);
		String URL_NODE1 = properties.getProperty("goldi_url");
		Properties sProp = new Properties();
		sProp.put("user", properties.getProperty("goldi_user"));
		sProp.put("password", properties.getProperty("goldi_password"));
		try {
			Class.forName("sunje.goldilocks.jdbc.GoldilocksDriver");
			con_goldi = DriverManager.getConnection(URL_NODE1, sProp);
			con_goldi.setAutoCommit(false);
			DatabaseMetaData md = con_goldi.getMetaData();

		} catch (SQLException | ClassNotFoundException e) {
			System.out.println(e.toString());
		}
		
		//read config
		try(FileInputStream fis = new FileInputStream("resource/config.json");
		    InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
		    BufferedReader reader = new BufferedReader(isr))
		{
		    Gson gson = new GsonBuilder().create();
		    Config config = gson.fromJson(reader, Config.class);
		    
		    //all print
		    System.out.println(config.toString());
		    //select print
		    System.out.println(config.host.name);
		    System.out.println(config.common[1].name);
		}
		//test 

	    String json = new GsonBuilder()
	               .setDateFormat("yyyy-MM-dd hh:mm:ss.SSS")
	               .create()
	               .toJson(new Timestamp(System.currentTimeMillis()));

	    System.out.println(json);
	}
}
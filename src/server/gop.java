package server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;


public class gop {
	public static void main(String[] args) throws FileNotFoundException, IOException {

		//read config
		try(FileInputStream fis = new FileInputStream("config.json");
		    InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
		    BufferedReader reader = new BufferedReader(isr))
		{
		    Gson gson = new GsonBuilder().create();
		    Config config = gson.fromJson(reader, Config.class);
		    System.out.println(config.host.name);
		}
		//test 

	    String json = new GsonBuilder()
	               .setDateFormat("yyyy-MM-dd hh:mm:ss.SSS")
	               .create()
	               .toJson(new Timestamp(System.currentTimeMillis()));

	    System.out.println(json);
	}
}
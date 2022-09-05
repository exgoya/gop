import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import model.ResultCommon;

public class LogCall {

	@SuppressWarnings("null")
	public void getHashMap(File file, Gson gson) throws JsonSyntaxException, IOException, ParseException {
		File rFile = file;

		FileInputStream fis = new FileInputStream(rFile);
		InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
		@SuppressWarnings("resource")
		BufferedReader reader = new BufferedReader(isr);

		String line = "";
	    LinkedHashMap<Timestamp, LinkedHashMap> timeMap = new LinkedHashMap<Timestamp, LinkedHashMap>();
	    LinkedHashMap<String,Integer  > nameMap = new LinkedHashMap<String, Integer>();
	    LinkedHashMap<String,ArrayList<String>   > tagMap = new LinkedHashMap<String, ArrayList<String> >();
	    ArrayList<String> tagList = new ArrayList<String>();
	    ArrayList<String> nameList = new ArrayList<String>();
 
	    int deb = 0;
		while ((line = reader.readLine()) != null) {

			ResultCommon rc = gson.fromJson(line, ResultCommon.class);
//			System.out.println(rc.toString());

//			if (rc.name.contains("session")) {
//				System.out.println(line);
//			}
		    String asdf = gson.fromJson(rc.timestamp,String.class);
	        DateTimeFormatter formatDateTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	        LocalDateTime localDateTime = LocalDateTime.from(formatDateTime.parse(asdf));
	        Timestamp ts = Timestamp.valueOf(localDateTime);

	        tagList.add(rc.tag);
//	        nameList.add(rc.name);
//	        tagMap.put(rc.tag,nameList);
	        
//	        Set<String> tagKeys = tagMap.keySet(); 
//			for (String key : tagKeys) {
//				tagMap.get(key);
//			}
//			
			nameMap.put(rc.name, rc.value);
			timeMap.put(ts, nameMap);
			
			//System.out.println(timeMap);
			//System.out.println(tagMap);
//		if(deb == 10)	{
//			
//			break;
//		}
//		deb++;
		}
			System.out.println(timeMap);
//
//		Set<String> tagKeys = tagMap.keySet();
//        for (String key : tagKeys) {
//            System.out.println(key + " -- "
//                               + tagMap.get(key));
//        }	
//        Set<Timestamp> timeKeys = timeMap.keySet();
//        for (Timestamp key : timeKeys) {
//            System.out.println(key + " -- "
//                               + timeMap.get(key));
//        }	
//        Set<String> nameKeys = nameMap.keySet();
//        for (String key : nameKeys) {
//            System.out.println(key + " -- "
//                               + nameMap.get(key));
//        }
	}
}

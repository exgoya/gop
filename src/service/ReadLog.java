package service;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Set;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import model.ResultCommon;

public class ReadLog {

	public LinkedHashMap<Timestamp, ResultCommon> timeMap = new LinkedHashMap<Timestamp, ResultCommon>();
	
	public LinkedHashMap<Timestamp, ResultCommon> getTimeMap() {
		return timeMap;
	}

	@Override
	public String toString() {
		Set<Timestamp> timeKeys = timeMap.keySet();
		String st="";
		for (Timestamp key : timeKeys) {
			st+=key + " -- " + timeMap.get(key) + "\n";
		}
		return st;
	}

	public void setTimeMap(File file, Gson gson) throws JsonSyntaxException, IOException, ParseException {
		File rFile = file;

		FileInputStream fis = new FileInputStream(rFile);
		InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
		@SuppressWarnings("resource")
		BufferedReader reader = new BufferedReader(isr);

		String line = "";

		while ((line = reader.readLine()) != null) {

			ResultCommon rc = gson.fromJson(line, ResultCommon.class);
			Timestamp ts = stringToTimestamp(rc.timestamp, gson);

			timeMap.put(ts, rc);
		}
	}

	private Timestamp stringToTimestamp(String timestamp, Gson gson) {
		// TODO Auto-generated method stub
		String tempTime = gson.fromJson(timestamp, String.class);
		DateTimeFormatter formatDateTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		LocalDateTime localDateTime = LocalDateTime.from(formatDateTime.parse(tempTime));
		Timestamp ts = Timestamp.valueOf(localDateTime);
		return ts;
	}
}

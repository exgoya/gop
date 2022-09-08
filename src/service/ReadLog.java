package service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Set;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import model.Config;
import model.ResultCommon;

public class ReadLog {

	public LinkedHashMap<LocalDateTime, ResultCommon[]> timeMap = new LinkedHashMap<LocalDateTime, ResultCommon[]>();
	public LinkedHashMap<LocalDateTime, ResultCommon[]> rangeTimeMap = new LinkedHashMap<LocalDateTime, ResultCommon[]>();
	public LinkedHashMap<LocalDateTime, ResultCommon[]> nameMap = new LinkedHashMap<LocalDateTime, ResultCommon[]>();
	public LinkedHashMap<LocalDateTime, ResultCommon[]> tagMap = new LinkedHashMap<LocalDateTime, ResultCommon[]>();

	public ReadLog(File file, Gson gson, Config config) throws JsonSyntaxException, IOException, ParseException {
		super();
		File rFile = file;

		FileInputStream fis = new FileInputStream(rFile);
		InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
		@SuppressWarnings("resource")
		BufferedReader reader = new BufferedReader(isr);

		String line = "";

		int i = 0;
		ResultCommon[] rc = new ResultCommon[config.common.length];
		;

		while ((line = reader.readLine()) != null) {
			ResultCommon obj = gson.fromJson(line, ResultCommon.class);
			LocalDateTime ts = stringToTimestamp(obj.timestamp, gson);
			if (i >= config.common.length) {
				rc = new ResultCommon[config.common.length];
				i = 0;
			}
			rc[i] = obj;
			timeMap.put(ts, rc);
			i++;
		}
	}

	public String convString(LinkedHashMap<LocalDateTime, ResultCommon[]> map) {
		Set<LocalDateTime> timeKeys = map.keySet();
		String st = "";
		for (LocalDateTime key : timeKeys) {
			ResultCommon[] rc = timeMap.get(key);
			for (int i = 0; i < rc.length; i++) {
				st += key + " -- " + rc[i].toString() + "\n";
			}
		}
		return st;
	}

	private LocalDateTime stringToTimestamp(String timestamp, Gson gson) {
		// TODO Auto-generated method stub
		String tempTime = gson.fromJson(timestamp, String.class);
		DateTimeFormatter formatDateTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		LocalDateTime localDateTime = LocalDateTime.from(formatDateTime.parse(tempTime));
		return localDateTime;
	}

	public void getTagList(String tag) {

	}

	public void setRangeTimeMap(LocalDateTime stTs, LocalDateTime edTs) {
		Set<LocalDateTime> timeKeys = timeMap.keySet();
		for (LocalDateTime key : timeKeys) {
			if (key.isAfter(stTs) || key.isEqual(stTs)) {
				if (key.isBefore(edTs) || key.isEqual(edTs)) {
					rangeTimeMap.put(key, timeMap.get(key));
				}
			}
		}
	}

	public void setNameMap(String name) {
		Set<LocalDateTime> timeKeys = timeMap.keySet();
		for (LocalDateTime key : timeKeys) {
			ResultCommon[] rc =timeMap.get(key);
			ResultCommon[] tempRc = new ResultCommon[1];
			for (int i = 0; i < rc.length; i++) {
				if(rc[i].name.equals(name)) {
					tempRc[0]=rc[i];
				};
			}
			nameMap.put(key, tempRc);
		}
	}

	public void setTagMap(String tag) {
		Set<LocalDateTime> timeKeys = timeMap.keySet();
		for (LocalDateTime key : timeKeys) {
			ResultCommon[] rc =timeMap.get(key);
			ResultCommon[] tempRc = new ResultCommon[1];
			for (int i = 0; i < rc.length; i++) {
				if(rc[i].name.equals(tag)) {
					tempRc[0]=rc[i];
				};
			}
			tagMap.put(key, tempRc);
		}
	}
}

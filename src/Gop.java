import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import model.Config;
import model.Logs;
import model.ResultCommon;
import service.Db;
import service.ReadLog;

public class Gop {
	static boolean gColumn = true;

	public static void main(String[] args)
			throws SQLException, IOException, InterruptedException, JsonSyntaxException, ParseException {

		File rFile = new File("resource/config.json");
		File wFile = new File("resource/out.json");

		// Gson gson = new GsonBuilder().setPrettyPrinting().create();
		Gson gson = new GsonBuilder().setLenient().create();
		Config config = readAndConvConf(rFile, Config.class, gson);
		// for test
		int i = 0;
		if (i == 1) {
			gStampLog(config,gson,wFile);
		}

		// gop client

		ReadLog rl = new ReadLog(new File("resource/out.json"), gson,config);
//		System.out.println(rl.toString());
		Set<LocalDateTime> timeKeys = rl.timeMap.keySet();

		String tag = "sql1";
		LocalDateTime stTs = stringToDate("2022-09-05 03:14:40");
		LocalDateTime edTs = stringToDate("2022-09-05 03:15:00");
		String name = "ager";

		rl.setRangeTime(stTs, edTs);
//		System.out.println(rl.convString(rl.rangeTimeMap));
		printTableMap(rl.rangeTimeMap);
		// rl.getTagList(tag);
//		rl.getNameList(name);
	}

	@SuppressWarnings("null")
	private static void printTableMap(LinkedHashMap<LocalDateTime, ResultCommon[]> rangeTimeMap) {
		Set<LocalDateTime> timeKeys = rangeTimeMap.keySet();
		ResultCommon[] rc = new ResultCommon[1] ;
		int i =0;
		
		for (LocalDateTime key : timeKeys) {
			rc=rangeTimeMap.get(key);
			i++;
			printTable(rc);
		}
	}

	private static LocalDateTime stringToDate(String startSearchKey) {

		DateTimeFormatter formatDateTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		LocalDateTime localDateTime = LocalDateTime.from(formatDateTime.parse(startSearchKey));
		return localDateTime;
	}

	private static void gStampLog(Config config ,Gson gson,File wFile )
			throws SQLException, IOException, InterruptedException {

		// db
		Db db = new Db(config);
		Connection con = db.createConnection();

		while (true) {

			ResultCommon[] rc = db.getCommonQuery(con);

			// write output file (json)
			writeJson(rc, gson, wFile);

			// print console (table)
			printTable(rc);

			Thread.sleep(1000);

		}
	}

	private static Config readAndConvConf(File rFile, Class<Config> class1, Gson gson) throws FileNotFoundException {
		FileInputStream fis = new FileInputStream(rFile);
		InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
		BufferedReader reader = new BufferedReader(isr);

		return gson.fromJson(reader, Config.class);
	}

	private static void writeJson(ResultCommon[] rc, Gson gson, File file) throws IOException {

		FileWriter fw = new FileWriter(file, true);
		BufferedWriter bw = new BufferedWriter(fw);
		for (ResultCommon resultCommon : rc) {
			String gRc = gson.toJson(resultCommon);
			bw.write(gRc);
			// bw.append(",");
			bw.newLine();
		}
		bw.close();
	}

	private static void printTable(ResultCommon[] rc) {
		// TODO Auto-generated method stub
		String[] column = new String[rc.length];
		int[] row = new int[rc.length];

		for (int i = 0; i < rc.length; i++) {
			column[i] = rc[i].name;
			row[i] = rc[i].value;
			// System.out.println(rc[i].toString());
		}

		int i = 0;
		if (gColumn == true) {
			System.out.format("%22s", "time");
			for ( i=0; i < row.length; i++) {
				System.out.format("%15s", column[i]);
			}
			System.out.format("%n");
			gColumn = false;
		}

		System.out.format("%22s", rc[rc.length-1].timestamp);
		
		for (int i1 = 0; i1 < row.length; i1++) {
			System.out.format("%15d", row[i1]);
		}
		System.out.format("%n");
	}

	private static String getTime() {
		// TODO Auto-generated method stub
		String sysTimeStamp = new GsonBuilder().setDateFormat("yyyy-MM-dd hh:mm:ss").create()
				.toJson(new Timestamp(System.currentTimeMillis()));
		return sysTimeStamp;
	}
}
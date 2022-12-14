import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import model.Common;
import model.Config;
import model.Data;
import model.ResultCommon;
import service.Db;
import service.ReadLog;
import service.ReadOs;
import service.CommandLineParser;

public class Gop {
	static boolean gColumn = true;
	static File rFile = null;
	static String gName = "";
	static String gHost = "";
	static String gPort = "";

	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI_BLACK = "\u001B[30m";
	public static final String ANSI_RED = "\u001B[31m";
	public static final String ANSI_GREEN = "\u001B[32m";
	public static final String ANSI_YELLOW = "\u001B[33m";
	public static final String ANSI_BLUE = "\u001B[34m";
	public static final String ANSI_PURPLE = "\u001B[35m";
	public static final String ANSI_CYAN = "\u001B[36m";
	public static final String ANSI_WHITE = "\u001B[37m";

	public static void main(String[] args)
			throws SQLException, IOException, InterruptedException, JsonSyntaxException, ParseException {
		CommandLineParser clp = new CommandLineParser(args);

		boolean help = clp.getFlag("help");
		if(help){
			System.out.println(" ---");
			System.out.println(" -config <config file path> [ -demon | -client -log <log file path> <option> ]");
			System.out.println(" ");
			System.out.println(" client option:");
			System.out.println("   -log <log file path> [ -time 'yyyy-mm-dd hh24:mi:ss.fff' 'yyyy-mm-dd hh24:mi:ss.fff' | -name <column name> | -tag <tag name> ]");
			System.out.println("			[ -head | -tail <print count> ]  ");
			System.out.println(" ");
			System.out.println(" ---");
			System.out.println(" sample use");
			System.out.println(" java -Xmx100M -jar gop.jar -config resource/config.json -demon  ");
			System.out.println(" java -jar gop.jar -config resource/config.json -client -log resource/log_20221201.json -time '2022-12-01 03:14:40.000' '2022-12-01 03:15:00.000'");
			System.out.println(" java -jar gop.jar -config resource/config.json -client -log resource/log_20221201.json -name execute -tail 10");
			System.out.println(" java -jar gop.jar -config resource/config.json -client -log resource/log_20221201.json -tag tag1 -head 10");
			System.out.println(" java -jar gop.jar -config resource/config.json -client -log resource/log_20221201.json");
			System.out.println(" ");

			System.exit(0);
		}
		if (args.length < 2) {
			System.out.println("invalid argument args : " + args.length);
			System.exit(0);
		}
		boolean client = clp.getFlag("client");
		boolean demon = clp.getFlag("demon");
		String configFile = clp.getArgumentValue("config")[0];
		String log = clp.getArgumentValue("log")[0];
		int head = clp.getArgumentValueInt("head");
		int tail = clp.getArgumentValueInt("tail");
		String time1 = clp.getArgumentValue("time")[0];
		String time2 = clp.getArgumentValue("time")[1];
		String tagArg = clp.getArgumentValue("tag")[0];
		String nameArg = clp.getArgumentValue("name")[0];



		rFile = new File(configFile);
		Gson gson = new GsonBuilder().setLenient().create();
		Config config = readAndConvConf(rFile, Config.class, gson);

		File logFile = new File(config.host.logPath + "log_" + getTime("YYYYMMdd") + ".json");
		System.out.println(logFile);
		File alertFile = new File(config.host.logPath + "alert_" + getTime("YYYYMM") + ".json");
		gName = config.host.name;
		gHost = config.host.ip;
		gPort = Integer.toString(config.host.port);

		if (demon) {
			gStampLog(config, gson, logFile, alertFile);
		} else if (client) {
			ReadLog rl = new ReadLog(new File(log), gson, config);

			if (time1 != null && time2 != null) {
				LocalDateTime stTs = stringToDate(time1);
				LocalDateTime edTs = stringToDate(time2);
				rl.setRangeTimeMap(stTs, edTs);
				printTableMap(rl.rangeTimeMap,head,tail);
			} else if (nameArg != null) {
				String name = nameArg;
				rl.setNameMap(name);
				printTableMap(rl.nameMap,head,tail);
			} else if (tagArg != null) {
				String tag = tagArg;
				rl.setTagMap(tag);
				printTableMap(rl.tagMap,head,tail);
			} else {
				printTableMap(rl.timeMap,head,tail);
			}
		} else {
			System.out.println("invalid argument");
		}

	}

//		System.out.println(rl.convString(rl.rangeTimeMap));

	private static String getTime(String string) {
		SimpleDateFormat sdf = new SimpleDateFormat(string);

		Calendar c1 = Calendar.getInstance();

		return sdf.format(c1.getTime());
	}

	private static void printTableMap(LinkedHashMap<LocalDateTime, ResultCommon[]> rangeTimeMap, int head, int tail) {
		if (rangeTimeMap.isEmpty()) {
			System.out.println("no data!");
			System.exit(0);
		}
		Set<LocalDateTime> timeKeys = rangeTimeMap.keySet();
		int i = 0;
		for (LocalDateTime key : timeKeys) {
			// System.out.println(key);
			if ( head > 0 ) {
				if(i < head ) {
					Data data = new Data(timestampToString(key), rangeTimeMap.get(key));
					printTable(data);
//					System.out.println("head: " +i);
				}
			}else if (tail > 0) {
				if(timeKeys.size()-tail <= i) {
					Data data = new Data(timestampToString(key), rangeTimeMap.get(key));
					printTable(data);
//					System.out.println("tail: " +i);
				}
			}else {
				Data data = new Data(timestampToString(key), rangeTimeMap.get(key));
				printTable(data);
			}
			i++;
		}
	}

	private static LocalDateTime stringToDate(String startSearchKey) {

		try {
			DateTimeFormatter formatDateTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
			LocalDateTime localDateTime = LocalDateTime.from(formatDateTime.parse(startSearchKey));
			return localDateTime;
		} catch (java.time.format.DateTimeParseException e) {
			// TODO: handle exceptionA
			System.out.println("invalid time : " + startSearchKey);
			System.exit(0);
		}
		return null;
	}

	private static void gStampLog(Config config, Gson gson, File logFile, File alertFile)
			throws SQLException, IOException, InterruptedException {

		// db
		Db db = new Db(config);
		Connection con = db.createConnection();
		PreparedStatement[] arrPstmt = db.createPstmt(con);
		int printRow = 0;
		Data beforeData = new Data(null, null);
		Data calData = new Data(null, null);

		while (true) {

			Data data = db.getCommonQuery(arrPstmt);

			// ResultCommon[] rc2 = db.getOsQuery();

			// write output file (json)
			if (beforeData.rc == null) {
				calData = data.newInstance(data);
				beforeData = data.newInstance(data);
			} else {
				calData = diffDataCal(data, beforeData, config);
				beforeData = data.newInstance(data);
			}

			writeJson(calData, gson, logFile, alertFile, config.host.logPath, config.common);
			// writeJson(rc2, gson, logFile, alertFile);

			// print console (table)
			if (config.host.print) {
				printTable(calData);
				printRow++;
				if (printRow % config.host.pagesize == 0) {
					gColumn = true;
				}
			}
			data = null;
			// rc2 = null;
			Thread.sleep(config.host.timeInterval);
		}
	}

	private static Data diffDataCal(Data data, Data beforeData, Config config) {
		// Data tempData = new Data(data.time, data.rc);
		// ResultCommon[] rc = new ResultCommon[data.rc.length];
		// Data tempData = new Data(data.time, rc);
		Data cal = data.newInstance(data);
		for (int i = 0; i < data.rc.length; i++) {
			if (config.common[i].diff) {
				cal.rc[i].value = data.rc[i].value - beforeData.rc[i].value;
			}
		}
		return cal;
	}

	private static String timestampToString(LocalDateTime timestamp) {
		// TODO Auto-generated method stub
		String tempTime = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss.SSS").create()
				.toJson(Timestamp.valueOf(timestamp));

		return tempTime;
	}

	private static Config readAndConvConf(File rFile, Class<Config> class1, Gson gson) throws FileNotFoundException {
		FileInputStream fis = new FileInputStream(rFile);
		InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
		BufferedReader reader = new BufferedReader(isr);

		return gson.fromJson(reader, Config.class);
	}

	private static void writeJson(Data data, Gson gson, File logFile, File alertFile, String logPath, Common[] common)
			throws IOException {

		if (!logFile.getName().equals(logPath + "log_" + getTime("YYYYMMdd") + ".json")) {
			logFile = new File(logPath + "log_" + getTime("YYYYMMdd") + ".json");
		}
		if (!alertFile.getName().equals(logPath + "alert_" + getTime("YYYYMM") + ".json")) {
			alertFile = new File(logPath + "alert_" + getTime("YYYYMM") + ".json");
		}
		FileWriter fw = new FileWriter(logFile, true);
		BufferedWriter bw = new BufferedWriter(fw);

		FileWriter alertFw = new FileWriter(alertFile, true);
		BufferedWriter alertBw = new BufferedWriter(alertFw);

//		for (ResultCommon resultCommon : data.rc) {
		String gRc = gson.toJson(data);
		bw.write(gRc);
		for (int i = 0; i < data.rc.length; i++) {
			if (data.rc[i].alert && common[i].alertScript != null) {
				alertBw.newLine();
				alertBw.write("alert time :" + data.time);
				alertBw.newLine();
				alertBw.newLine();
				if (common[i].alertScriptIsOs) {
					alertBw.write(ReadOs.executeS(common[i].alertScript));
				} else {
					String tmp = "echo \'" + common[i].alertScript + ";\' |gsqlnet sys gliese --no-prompt";
					alertBw.write(ReadOs.executeS(tmp));
				}
				alertBw.newLine();
			}
		}
		// bw.append(",");
		bw.newLine();
		bw.close();
		alertBw.close();
	}

	private static void printTable(Data data) {
		// TODO Auto-generated method stub
		String[] column = new String[data.rc.length];
		String[] row = new String[data.rc.length];

		for (int i = 0; i < data.rc.length; i++) {
			if (data.rc[i] != null) {
				column[i] = data.rc[i].name;
				row[i] = alertFormat(data.rc[i].value, data.rc[i].alert);
				// System.out.println(rc[i].toString());
			}
		}

		int i = 0;
		if (gColumn == true) {
//			System.out.println("** instance name :" + gName);

			System.out.format("%24s", "NAME : " + ANSI_PURPLE + gName + ANSI_RESET);
			System.out.format("%28s", "HOST : " + ANSI_PURPLE + gHost + ANSI_RESET);
			System.out.format("%24s", "PORT : " + ANSI_PURPLE + gPort + ANSI_RESET);

			System.out.println("");
			System.out.format("%34s", ANSI_GREEN + "time" + ANSI_RESET);
			for (i = 0; i < row.length; i++) {
				if (column[i] != null) {
					System.out.format("%23s", ANSI_GREEN + column[i] + ANSI_RESET);
				}
			}
			System.out.format("%n");
			gColumn = false;
		}

		for (i = 0; i < data.rc.length; i++) {
			if (data.rc[i] != null) {
				// System.out.println(rc[i].toString());
				System.out.format("%22s", ANSI_GREEN + data.time + ANSI_RESET);
				break;
			}
		}

		for (i = 0; i < row.length; i++) {
			if (row[i] != null) {
				System.out.format("%23s", row[i]);
			}
		}
		System.out.format("%n");
	}

	private static String alertFormat(int value, boolean alert) {
		String temp = null;
		if (alert) {
			temp = ANSI_RED + String.valueOf(value) + ANSI_RESET;
		} else {
			temp = ANSI_WHITE + String.valueOf(value) + ANSI_RESET;
		}
		return temp;
	}
}

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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Gop {
	static boolean gColumn = true;

	public static void main(String[] args)
			throws FileNotFoundException, IOException, SQLException, InterruptedException {

		File rFile = new File("resource/config.json");
		File wFile = new File("resource/out.json");
		
		// Gson gson = new GsonBuilder().setPrettyPrinting().create();
		Gson gson = new GsonBuilder().create();	
		
		// read config
		Config config = readAndConv(rFile,Config.class,gson);

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

	private static Config readAndConv(File rFile, Class<Config> class1, Gson gson) throws FileNotFoundException {
		FileInputStream fis = new FileInputStream(rFile);
		InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
		BufferedReader reader = new BufferedReader(isr);

		return gson.fromJson(reader, Config.class);
	}

	private static void writeJson(ResultCommon[] rc, Gson gson, File file) throws IOException {

		String gRc = gson.toJson(rc);

		FileWriter fw = new FileWriter(file, true);
		BufferedWriter bw = new BufferedWriter(fw);
		bw.write(gRc);
		bw.newLine();
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

		if (gColumn == true) {
			System.out.format("%30s", "time");
			for (int i = 0; i < row.length; i++) {
				System.out.format("%15s", column[i]);
			}
			System.out.format("%n");
			gColumn = false;
		}

		System.out.format("%30s", getTime());
		for (int i = 0; i < row.length; i++) {
			System.out.format("%15d", row[i]);
		}
		System.out.format("%n");
	}

	public static boolean writeFile(File file, boolean append, byte[] file_content) {
		boolean result;
		FileOutputStream fos;
		if (file != null && file.exists() && file_content != null) {
			try {
				fos = new FileOutputStream(file, append);
				try {
					fos.write(file_content);
					fos.flush();
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			result = true;
		} else {
			result = false;
		}
		return result;
	}

	private static String getTime() {
		// TODO Auto-generated method stub
		String sysTimeStamp = new GsonBuilder().setDateFormat("yyyy-MM-dd hh:mm:ss").create()
				.toJson(new Timestamp(System.currentTimeMillis()));
		return sysTimeStamp;
	}
}

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
	public static void main(String[] args)
			throws FileNotFoundException, IOException, SQLException, InterruptedException {

		boolean first = true;

		// read config
		try (FileInputStream fis = new FileInputStream("resource/config.json");
				InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
				BufferedReader reader = new BufferedReader(isr)) {
			// Gson gson = new GsonBuilder().setPrettyPrinting().create();
			Gson gson = new GsonBuilder().create();
			Config config = gson.fromJson(reader, Config.class);

			// db
			Db db = new Db(config);
			Connection con = db.createConnection();

			File file = new File("resource/out.json");
		    
			while (true) {

				ResultCommon[] rc = db.getCommonQuery(con);

				// write output file (json)
				String gRc = gson.toJson(rc);

				FileWriter fw = new FileWriter(file, true);
			    BufferedWriter bw = new BufferedWriter(fw);
			    bw.write(gRc);
			    bw.newLine();
			    bw.close();
				// print console (table)
				String[] column = new String[rc.length];
				int[] row = new int[rc.length];

				for (int i = 0; i < rc.length; i++) {
					column[i] = rc[i].name;
					row[i] = rc[i].value;
					// System.out.println(rc[i].toString());
				}

				if (first == true) {
					System.out.format("%30s", "time");
					for (int i = 0; i < row.length; i++) {
						System.out.format("%15s", column[i]);
					}
					System.out.format("%n");
					first = false;
				}

				System.out.format("%30s", getTime());
				for (int i = 0; i < row.length; i++) {
					System.out.format("%15d", row[i]);
				}
				System.out.format("%n");
				Thread.sleep(1000);
			}
		}
	}

	// test
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
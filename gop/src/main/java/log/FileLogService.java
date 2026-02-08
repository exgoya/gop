package log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import com.google.gson.Gson;

import config.ConfigManager;
import io.ReadOs;
import model.Config;
import model.Data;
import model.Measure;

public final class FileLogService {
	private static final long DEFAULT_MAX_LOG_BYTES = 50L * 1024 * 1024; // 50MB
	private static final int DEFAULT_MAX_LOG_BACKUPS = 5;

	private FileLogService() {
	}

	public static void writeJson(Data data, Gson gson, Config config, Measure[] measure) throws IOException {
		String basePath = ConfigManager.resolveLogBasePath(config);
		long maxBytes = DEFAULT_MAX_LOG_BYTES;
		int maxBackups = DEFAULT_MAX_LOG_BACKUPS;
		if (config != null && config.setting != null && config.setting.fileLog != null) {
			if (config.setting.fileLog.maxBytes != null && config.setting.fileLog.maxBytes > 0) {
				maxBytes = config.setting.fileLog.maxBytes;
			}
			if (config.setting.fileLog.maxBackups != null && config.setting.fileLog.maxBackups > 0) {
				maxBackups = config.setting.fileLog.maxBackups;
			}
		}
		String ymPath = getTime("yyyy/MM") + "/";
		ConfigManager.validateId("source id", data.source);
		String sourcePath = data.source == null || data.source.trim().isEmpty() ? "default" : data.source.trim();
		String dirPath = basePath + ymPath + sourcePath + "/";
		File logDir = new File(dirPath);
		if (!logDir.exists()) {
			logDir.mkdirs();
		}

		File logFile = new File(dirPath + "log_" + getTime("yyyyMMdd") + ".json");
		File alertFile = new File(dirPath + "alert_" + getTime("yyyyMM") + ".json");

		if (!logFile.getName().equals(dirPath + "log_" + getTime("yyyyMMdd") + ".json")) {
			logFile = new File(dirPath + "log_" + getTime("yyyyMMdd") + ".json");
		}
		if (!alertFile.getName().equals(dirPath + "alert_" + getTime("yyyyMM") + ".json")) {
			alertFile = new File(dirPath + "alert_" + getTime("yyyyMM") + ".json");
		}
		rotateIfNeeded(logFile, maxBytes, maxBackups);
		rotateIfNeeded(alertFile, maxBytes, maxBackups);

		BufferedWriter bw = new BufferedWriter(new FileWriter(logFile, true));

		String gRc = gson.toJson(data);
		bw.write(gRc);

		Measure[] ms = measure;

		BufferedWriter alertBw = null;
		try {
			for (int i = 0; i < data.rc.length; i++) {
				if (ms == null || i >= ms.length || ms[i] == null) {
					continue;
				}
				if (data.rc[i].alert && ms[i].alertScript != null) {
					if (alertBw == null) {
						FileWriter alertFw = new FileWriter(alertFile, true);
						alertBw = new BufferedWriter(alertFw);
					}
					alertBw.newLine();
					alertBw.write("alert time :" + data.time);
					alertBw.newLine();
					alertBw.newLine();
					if (ms[i].alertScriptIsOs) {
						alertBw.write(ReadOs.executeS(ms[i].alertScript));
					} else {
						String tmp = "echo \'" + ms[i].alertScript + ";\' |gsqlnet sys gliese --no-prompt";
						alertBw.write(ReadOs.executeS(tmp));
					}
					alertBw.newLine();
				}
			}
		} finally {
			if (alertBw != null) {
				alertBw.close();
			}
		}

		bw.newLine();
		bw.close();
	}

	private static String getTime(String string) {
		SimpleDateFormat sdf = new SimpleDateFormat(string);
		Calendar c1 = Calendar.getInstance();
		return sdf.format(c1.getTime());
	}

	private static void rotateIfNeeded(File file, long maxBytes, int maxBackups) throws IOException {
		if (!file.exists() || file.length() < maxBytes) {
			return;
		}
		for (int i = maxBackups; i >= 1; i--) {
			File target = new File(file.getAbsolutePath() + "." + i + ".gz");
			if (target.exists()) {
				if (i == maxBackups) {
					target.delete();
				} else {
					File next = new File(file.getAbsolutePath() + "." + (i + 1) + ".gz");
					target.renameTo(next);
				}
			}
		}
		File rotated = new File(file.getAbsolutePath() + ".1");
		if (rotated.exists()) {
			rotated.delete();
		}
		if (!file.renameTo(rotated)) {
			return;
		}
		gzipFile(rotated, new File(rotated.getAbsolutePath() + ".gz"));
		rotated.delete();
	}

	private static void gzipFile(File source, File target) throws IOException {
		try (java.io.FileInputStream fis = new java.io.FileInputStream(source);
				java.io.FileOutputStream fos = new java.io.FileOutputStream(target);
				java.util.zip.GZIPOutputStream gos = new java.util.zip.GZIPOutputStream(fos)) {
			byte[] buffer = new byte[8192];
			int len;
			while ((len = fis.read(buffer)) > 0) {
				gos.write(buffer, 0, len);
			}
		}
	}
}

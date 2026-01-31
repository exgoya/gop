package log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

public final class LogFileUtil {
	private LogFileUtil() {
	}

	public static BufferedReader openLogReader(File logFile, File gzFile) throws IOException {
		InputStream input = logFile != null && logFile.exists()
				? new FileInputStream(logFile)
				: new java.util.zip.GZIPInputStream(new FileInputStream(gzFile));
		return new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
	}

	public static String readLastNonEmptyLineFast(File file, int maxBytes) throws IOException {
		if (file == null || !file.exists()) {
			return null;
		}
		long len = file.length();
		if (len <= maxBytes) {
			return readLastNonEmptyLine(file);
		}
		long pos = len;
		int block = 16 * 1024;
		String tail = null;
		try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
			while (pos > 0) {
				long start = Math.max(0, pos - block);
				int size = (int) (pos - start);
				byte[] buf = new byte[size];
				raf.seek(start);
				raf.readFully(buf);
				String chunk = new String(buf, StandardCharsets.UTF_8);
				if (tail != null) {
					chunk = chunk + tail;
				}
				String[] lines = chunk.split("\n");
				for (int i = lines.length - 1; i >= 0; i--) {
					String line = lines[i].trim();
					if (!line.isEmpty()) {
						return line;
					}
				}
				tail = chunk;
				pos = start;
				if (len - pos > maxBytes) {
					break;
				}
			}
		}
		return null;
	}

	public static String readLastNonEmptyLine(File file) throws IOException {
		String last = null;
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (!line.trim().isEmpty()) {
					last = line;
				}
			}
		}
		return last;
	}

	public static String readLastNonEmptyLineGzip(File file) throws IOException {
		String last = null;
		try (java.util.zip.GZIPInputStream gis = new java.util.zip.GZIPInputStream(new FileInputStream(file));
				BufferedReader reader = new BufferedReader(new InputStreamReader(gis, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (!line.trim().isEmpty()) {
					last = line;
				}
			}
		}
		return last;
	}
}

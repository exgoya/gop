package api;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import config.ConfigManager;
import log.LogFileUtil;
import model.Config;
import model.Data;
import model.ResultCommon;
import model.SourceConfig;

public final class ApiServer {
	private static final int STATUS_TAIL_BYTES = 64 * 1024;

	private ApiServer() {
	}

	public static void startIfEnabled(Config config) throws IOException {
		if (config.setting.api == null || !config.setting.api.enable) {
			return;
		}
		int port = config.setting.api.port > 0 ? config.setting.api.port : 18080;
		int poolSize = config.setting.api.threadPoolSize > 0 ? config.setting.api.threadPoolSize : 4;
		PrintWriter apiLog = createApiLogger(config);
		com.sun.net.httpserver.HttpServer server = com.sun.net.httpserver.HttpServer
				.create(new InetSocketAddress("127.0.0.1", port), 0);
		java.util.concurrent.ExecutorService apiExecutor = java.util.concurrent.Executors.newFixedThreadPool(poolSize);
		server.setExecutor(apiExecutor);
		server.createContext("/health", exchange -> {
			logApi(apiLog, "health", exchange.getRequestMethod(), exchange.getRemoteAddress().toString(), 200);
			byte[] body = "ok".getBytes(java.nio.charset.StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(200, body.length);
			try (java.io.OutputStream os = exchange.getResponseBody()) {
				os.write(body);
			}
		});
		server.createContext("/status", exchange -> {
			if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
				logApi(apiLog, "status", exchange.getRequestMethod(), exchange.getRemoteAddress().toString(), 405);
				exchange.sendResponseHeaders(405, -1);
				return;
			}
			String body = readRequestBody(exchange.getRequestBody());
			Gson gson = new GsonBuilder().setLenient().create();
			StatusQuery query = body.isEmpty() ? new StatusQuery() : gson.fromJson(body, StatusQuery.class);
			String resultJson;
			try {
				resultJson = readStatusAsJson(config, gson, query);
			} catch (Exception e) {
				logApi(apiLog, "status", exchange.getRequestMethod(), exchange.getRemoteAddress().toString(), 500);
				byte[] err = e.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
				exchange.sendResponseHeaders(500, err.length);
				try (java.io.OutputStream os = exchange.getResponseBody()) {
					os.write(err);
				}
				return;
			}
			logApi(apiLog, "status", exchange.getRequestMethod(), exchange.getRemoteAddress().toString(), 200);
			byte[] out = resultJson.getBytes(java.nio.charset.StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, out.length);
			try (java.io.OutputStream os = exchange.getResponseBody()) {
				os.write(out);
			}
		});
		server.createContext("/watch", exchange -> {
			if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
				logApi(apiLog, "watch", exchange.getRequestMethod(), exchange.getRemoteAddress().toString(), 405);
				exchange.sendResponseHeaders(405, -1);
				return;
			}
			InputStream is = exchange.getRequestBody();
			String body = readRequestBody(is);
			Gson gson = new GsonBuilder().setLenient().create();
			WatchQuery query = gson.fromJson(body, WatchQuery.class);
			if (query == null || query.source == null) {
				logApi(apiLog, "watch", exchange.getRequestMethod(), exchange.getRemoteAddress().toString(), 400);
				exchange.sendResponseHeaders(400, -1);
				return;
			}
			String resultJson;
			try {
				resultJson = readLogsAsJson(config, gson, query);
			} catch (Exception e) {
				logApi(apiLog, "watch", exchange.getRequestMethod(), exchange.getRemoteAddress().toString(), 500);
				byte[] err = e.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
				exchange.sendResponseHeaders(500, err.length);
				try (java.io.OutputStream os = exchange.getResponseBody()) {
					os.write(err);
				}
				return;
			}
			logApi(apiLog, "watch", exchange.getRequestMethod(), exchange.getRemoteAddress().toString(), 200);
			byte[] out = resultJson.getBytes(java.nio.charset.StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, out.length);
			try (java.io.OutputStream os = exchange.getResponseBody()) {
				os.write(out);
			}
		});
		server.start();
		System.out.println("API server listening on http://127.0.0.1:" + port);
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			server.stop(0);
			apiExecutor.shutdown();
			if (apiLog != null) {
				apiLog.flush();
				apiLog.close();
			}
		}));
	}

	private static class WatchQuery {
		String source;
		String timeFrom;
		String timeTo;
		String name;
		String tag;
		Integer head;
		Integer tail;
	}

	private static class StatusQuery {
		String source;
		String name;
		String tag;
	}

	private static PrintWriter createApiLogger(Config config) throws IOException {
		if (config.setting.api == null || config.setting.api.logPath == null || config.setting.api.logPath.isEmpty()) {
			return null;
		}
		java.io.File logFile = new java.io.File(config.setting.api.logPath);
		java.io.File parent = logFile.getParentFile();
		if (parent != null && !parent.exists()) {
			parent.mkdirs();
		}
		return new PrintWriter(new java.io.FileWriter(logFile, true), true);
	}

	private static void logApi(PrintWriter writer, String path, String method, String remote, int status) {
		if (writer == null) {
			return;
		}
		String ts = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").format(LocalDateTime.now());
		writer.printf("%s %s %s %s %d%n", ts, path, method, remote, status);
	}

	private static String readRequestBody(InputStream is) throws IOException {
		java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
		byte[] buf = new byte[8192];
		int n;
		while ((n = is.read(buf)) > 0) {
			baos.write(buf, 0, n);
		}
		return baos.toString(java.nio.charset.StandardCharsets.UTF_8).trim();
	}

	private static String readStatusAsJson(Config config, Gson gson, StatusQuery query) throws IOException {
		List<Data> results = new ArrayList<>();
		List<String> sources = new ArrayList<>();
		if (query != null && query.source != null && !query.source.isEmpty()) {
			sources.add(query.source);
		} else if (config.sources != null && config.sources.length > 0) {
			for (SourceConfig sc : config.sources) {
				sources.add(sc.source);
			}
		} else {
			SourceConfig sc = buildSingleSource(config);
			sources.add(sc.source);
		}
		for (String src : sources) {
			validateSourceDir(src);
			Data data = readLatestLog(config, gson, src);
			if (data == null) {
				continue;
			}
			if (query != null && (query.name != null || query.tag != null)) {
				List<ResultCommon> filtered = new ArrayList<>();
				if (data.rc != null) {
					for (ResultCommon rc : data.rc) {
						if (rc == null) {
							continue;
						}
						if (query.name != null && !query.name.equals(rc.measure)) {
							continue;
						}
						if (query.tag != null && !query.tag.equals(rc.tag)) {
							continue;
						}
						filtered.add(rc);
					}
				}
				if (filtered.isEmpty()) {
					continue;
				}
				data.rc = filtered.toArray(new ResultCommon[0]);
			}
			results.add(data);
		}
		return gson.toJson(results);
	}

	private static Data readLatestLog(Config config, Gson gson, String source) throws IOException {
		LocalDate today = LocalDate.now();
		DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("yyyyMMdd");
		DateTimeFormatter ymFmt = DateTimeFormatter.ofPattern("yyyy/MM");
		String basePath = ConfigManager.resolveLogBasePath(config);
		for (int i = 0; i < 30; i++) {
			LocalDate d = today.minusDays(i);
			String dir = basePath + ymFmt.format(d) + "/" + source + "/";
			java.io.File logFile = new java.io.File(dir + "log_" + dayFmt.format(d) + ".json");
			java.io.File gzFile = new java.io.File(dir + "log_" + dayFmt.format(d) + ".json.gz");
			if (!logFile.exists() && !gzFile.exists()) {
				continue;
			}
			String lastLine = logFile.exists()
					? LogFileUtil.readLastNonEmptyLineFast(logFile, STATUS_TAIL_BYTES)
					: LogFileUtil.readLastNonEmptyLineGzip(gzFile);
			if (lastLine == null) {
				continue;
			}
			try {
				return gson.fromJson(lastLine, Data.class);
			} catch (Exception e) {
				continue;
			}
		}
		return null;
	}

	private static String readLogsAsJson(Config config, Gson gson, WatchQuery query) throws IOException {
		DateTimeFormatter tsFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
		LocalDateTime from;
		LocalDateTime to;
		if (query.timeFrom != null && !query.timeFrom.isEmpty()) {
			from = LocalDateTime.from(tsFormat.parse(query.timeFrom));
		} else {
			from = null;
		}
		if (query.timeTo != null && !query.timeTo.isEmpty()) {
			to = LocalDateTime.from(tsFormat.parse(query.timeTo));
		} else {
			to = null;
		}
		if (from == null && to == null) {
			LocalDate today = LocalDate.now();
			from = today.atStartOfDay();
			to = today.plusDays(1).atStartOfDay().minusNanos(1);
		} else if (from == null) {
			from = to;
		} else if (to == null) {
			to = from;
		}
		List<Data> results = new ArrayList<>();
		java.util.Deque<Data> tailQueue = null;
		boolean useHead = query.head != null && query.head > 0;
		boolean useTail = query.tail != null && query.tail > 0 && !useHead;
		if (useTail) {
			tailQueue = new java.util.LinkedList<>();
		}
		LocalDate startDate = from.toLocalDate();
		LocalDate endDate = to.toLocalDate();
		DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("yyyyMMdd");
		DateTimeFormatter ymFmt = DateTimeFormatter.ofPattern("yyyy/MM");
		String basePath = ConfigManager.resolveLogBasePath(config);
		String source = query.source;
		validateSourceDir(source);
		String sourceDir = source.trim();
		int badLines = 0;
		for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
			String dir = basePath + ymFmt.format(d) + "/" + sourceDir + "/";
			java.io.File logFile = new java.io.File(dir + "log_" + dayFmt.format(d) + ".json");
			java.io.File gzFile = new java.io.File(dir + "log_" + dayFmt.format(d) + ".json.gz");
			if (!logFile.exists() && !gzFile.exists()) {
				continue;
			}
			try (java.io.BufferedReader reader = LogFileUtil.openLogReader(logFile, gzFile)) {
				String line;
				while ((line = reader.readLine()) != null) {
					Data obj;
					try {
						obj = gson.fromJson(line, Data.class);
					} catch (Exception e) {
						badLines++;
						continue;
					}
					if (obj == null || obj.time == null) {
						continue;
					}
					LocalDateTime ts;
					try {
						ts = LocalDateTime.from(tsFormat.parse(obj.time));
					} catch (Exception e) {
						badLines++;
						continue;
					}
					if (ts.isBefore(from) || ts.isAfter(to)) {
						continue;
					}
					if (query.name != null || query.tag != null) {
						List<ResultCommon> filtered = new ArrayList<>();
						if (obj.rc != null) {
							for (ResultCommon rc : obj.rc) {
								if (rc == null) {
									continue;
								}
								if (query.name != null && !query.name.equals(rc.measure)) {
									continue;
								}
								if (query.tag != null && !query.tag.equals(rc.tag)) {
									continue;
								}
								filtered.add(rc);
							}
						}
						if (filtered.isEmpty()) {
							continue;
						}
						obj.rc = filtered.toArray(new ResultCommon[0]);
					}
					if (useHead) {
						results.add(obj);
						if (results.size() >= query.head) {
							break;
						}
					} else if (useTail) {
						tailQueue.addLast(obj);
						while (tailQueue.size() > query.tail) {
							tailQueue.removeFirst();
						}
					} else {
						results.add(obj);
					}
				}
			}
			if (useHead && results.size() >= query.head) {
				break;
			}
		}
		if (useTail && tailQueue != null) {
			results = new ArrayList<>(tailQueue);
		}
		if (badLines > 0) {
			System.err.println("skip invalid json lines: " + badLines);
		}
		return gson.toJson(results);
	}

	private static void validateSourceDir(String source) {
		ConfigManager.validateId("source id", source);
	}

	private static SourceConfig buildSingleSource(Config config) {
		SourceConfig source = new SourceConfig();
		source.source = config.setting.source == null ? "default" : config.setting.source;
		source.jdbcSource = config.setting.jdbcSource;
		source.measure = config.measure;
		return source;
	}
}

package api;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import config.ConfigManager;
import log.LogFileUtil;
import model.Config;
import model.Data;
import model.ResultCommon;
import model.SourceConfig;

public final class ApiServer {
	private static final int STATUS_TAIL_BYTES = 64 * 1024;
	private static final Pattern SAFE_NAME = Pattern.compile("[A-Za-z0-9._-]+");

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
		server.createContext("/dashboard", exchange -> {
			if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
				logApi(apiLog, "dashboard", exchange.getRequestMethod(), exchange.getRemoteAddress().toString(), 405);
				exchange.sendResponseHeaders(405, -1);
				return;
			}
			String body = readRequestBody(exchange.getRequestBody());
			Gson gson = new GsonBuilder().setLenient().create();
			DashboardQuery query = body.isEmpty() ? new DashboardQuery() : gson.fromJson(body, DashboardQuery.class);
			String resultJson;
			try {
				resultJson = readDashboardAsJson(config, gson, query);
			} catch (Exception e) {
				logApi(apiLog, "dashboard", exchange.getRequestMethod(), exchange.getRemoteAddress().toString(), 500);
				byte[] err = e.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
				exchange.sendResponseHeaders(500, err.length);
				try (java.io.OutputStream os = exchange.getResponseBody()) {
					os.write(err);
				}
				return;
			}
			logApi(apiLog, "dashboard", exchange.getRequestMethod(), exchange.getRemoteAddress().toString(), 200);
			byte[] out = resultJson.getBytes(java.nio.charset.StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, out.length);
			try (java.io.OutputStream os = exchange.getResponseBody()) {
				os.write(out);
			}
		});
		server.createContext("/dashboard/series", exchange -> {
			if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
				logApi(apiLog, "dashboard/series", exchange.getRequestMethod(), exchange.getRemoteAddress().toString(),
						405);
				exchange.sendResponseHeaders(405, -1);
				return;
			}
			String body = readRequestBody(exchange.getRequestBody());
			Gson gson = new GsonBuilder().setLenient().create();
			DashboardSeriesQuery query = body.isEmpty() ? new DashboardSeriesQuery()
					: gson.fromJson(body, DashboardSeriesQuery.class);
			String resultJson;
			try {
				resultJson = readDashboardSeriesAsJson(config, gson, query);
			} catch (Exception e) {
				logApi(apiLog, "dashboard/series", exchange.getRequestMethod(), exchange.getRemoteAddress().toString(),
						500);
				byte[] err = e.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
				exchange.sendResponseHeaders(500, err.length);
				try (java.io.OutputStream os = exchange.getResponseBody()) {
					os.write(err);
				}
				return;
			}
			logApi(apiLog, "dashboard/series", exchange.getRequestMethod(), exchange.getRemoteAddress().toString(), 200);
			byte[] out = resultJson.getBytes(java.nio.charset.StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, out.length);
			try (java.io.OutputStream os = exchange.getResponseBody()) {
				os.write(out);
			}
		});
		server.createContext("/ui/dashboard", exchange -> {
			if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
				logApi(apiLog, "ui/dashboard", exchange.getRequestMethod(), exchange.getRemoteAddress().toString(), 405);
				exchange.sendResponseHeaders(405, -1);
				return;
			}
			logApi(apiLog, "ui/dashboard", exchange.getRequestMethod(), exchange.getRemoteAddress().toString(), 200);
			byte[] out = dashboardHtml().getBytes(java.nio.charset.StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
			exchange.sendResponseHeaders(200, out.length);
			try (java.io.OutputStream os = exchange.getResponseBody()) {
				os.write(out);
			}
		});
		server.createContext("/config/server", exchange -> handleConfigEndpoint(apiLog, exchange, "server"));
		server.createContext("/config/source", exchange -> handleConfigEndpoint(apiLog, exchange, "source"));
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

	private static class DashboardQuery {
		String source;
		String name;
		String tag;
		String timeFrom;
		String timeTo;
		Integer limit;
	}

	private static class DashboardSeriesQuery {
		String source;
		String name;
		String tag;
		String timeFrom;
		String timeTo;
		Integer limit;
	}

	private static class ConfigUpsertRequest {
		String name;
		JsonElement config;
	}

	private static class ConfigUpsertResponse {
		String kind;
		String name;
		String path;
		boolean saved;
	}

	private static class ConfigListResponse {
		String kind;
		String rootPath;
		List<String> names;
	}

	private static class DashboardResponse {
		String timeFrom;
		String timeTo;
		List<DashboardItem> items;
	}

	private static class DashboardItem {
		String source;
		String measure;
		String latestTime;
		long latestValue;
		long minValue;
		long maxValue;
		double avgValue;
		long count;
		long alertCount;
		long actionCount;
		String[] latestActionStates;
	}

	private static class DashboardAgg {
		String source;
		String measure;
		String latestTime;
		LocalDateTime latestTs;
		long latestValue;
		long minValue;
		long maxValue;
		double sum;
		long count;
		long alertCount;
		long actionCount;
		String[] latestActionStates;
	}

	private static class DashboardSeriesResponse {
		String timeFrom;
		String timeTo;
		List<DashboardPoint> points;
	}

	private static class DashboardPoint {
		String time;
		String source;
		String measure;
		long value;
		String tag;
		boolean alert;
		String target;
		String[] actionStates;
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
		return baos.toString(StandardCharsets.UTF_8).trim();
	}

	private static void handleConfigEndpoint(PrintWriter apiLog, com.sun.net.httpserver.HttpExchange exchange, String kind)
			throws IOException {
		String method = exchange.getRequestMethod();
		Gson gson = new GsonBuilder().setLenient().create();
		try {
			if ("GET".equalsIgnoreCase(method)) {
				ConfigListResponse response = listConfigFiles(kind);
				String json = gson.toJson(response);
				sendResponse(exchange, 200, "application/json", json);
				logApi(apiLog, "config/" + kind, method, exchange.getRemoteAddress().toString(), 200);
				return;
			}
			if ("POST".equalsIgnoreCase(method)) {
				String body = readRequestBody(exchange.getRequestBody());
				ConfigUpsertResponse response = upsertConfigFile(kind, gson, body);
				String json = gson.toJson(response);
				sendResponse(exchange, 200, "application/json", json);
				logApi(apiLog, "config/" + kind, method, exchange.getRemoteAddress().toString(), 200);
				return;
			}
			logApi(apiLog, "config/" + kind, method, exchange.getRemoteAddress().toString(), 405);
			exchange.sendResponseHeaders(405, -1);
		} catch (IllegalArgumentException e) {
			logApi(apiLog, "config/" + kind, method, exchange.getRemoteAddress().toString(), 400);
			sendResponse(exchange, 400, "text/plain", e.getMessage());
		} catch (Exception e) {
			logApi(apiLog, "config/" + kind, method, exchange.getRemoteAddress().toString(), 500);
			sendResponse(exchange, 500, "text/plain", e.toString());
		}
	}

	private static void sendResponse(com.sun.net.httpserver.HttpExchange exchange, int status, String contentType,
			String body) throws IOException {
		byte[] out = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=utf-8");
		exchange.sendResponseHeaders(status, out.length);
		try (java.io.OutputStream os = exchange.getResponseBody()) {
			os.write(out);
		}
	}

	private static ConfigUpsertResponse upsertConfigFile(String kind, Gson gson, String body) throws IOException {
		if (body == null || body.trim().isEmpty()) {
			throw new IllegalArgumentException("request body is required");
		}
		JsonElement parsed = JsonParser.parseString(body);
		if (!parsed.isJsonObject()) {
			throw new IllegalArgumentException("request body must be a json object");
		}
		JsonObject root = parsed.getAsJsonObject();
		ConfigUpsertRequest request = gson.fromJson(root, ConfigUpsertRequest.class);

		JsonObject configObject;
		if (request != null && request.config != null) {
			if (!request.config.isJsonObject()) {
				throw new IllegalArgumentException("'config' must be a json object");
			}
			configObject = request.config.getAsJsonObject();
		} else {
			configObject = root;
		}
		String name = resolveConfigName(kind, request, configObject);
		validateConfigName(name);
		validateConfigShape(kind, configObject);

		Path dir = resolveConfigDir(kind);
		Files.createDirectories(dir);
		Path target = dir.resolve(name + ".json").normalize();
		if (!target.startsWith(dir)) {
			throw new IllegalArgumentException("invalid config path");
		}
		Gson pretty = new GsonBuilder().setPrettyPrinting().create();
		String content = pretty.toJson(configObject) + System.lineSeparator();
		Files.writeString(target, content, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
				StandardOpenOption.TRUNCATE_EXISTING);

		ConfigUpsertResponse response = new ConfigUpsertResponse();
		response.kind = kind;
		response.name = name;
		response.path = target.toAbsolutePath().toString();
		response.saved = true;
		return response;
	}

	private static ConfigListResponse listConfigFiles(String kind) throws IOException {
		Path dir = resolveConfigDir(kind);
		List<String> names = new ArrayList<>();
		if (Files.exists(dir)) {
			try (java.util.stream.Stream<Path> stream = Files.list(dir)) {
				stream.filter(Files::isRegularFile).map(Path::getFileName).map(Path::toString)
						.filter(n -> n.endsWith(".json")).sorted().forEach(n -> names.add(n.substring(0, n.length() - 5)));
			}
		}
		ConfigListResponse response = new ConfigListResponse();
		response.kind = kind;
		response.rootPath = dir.toAbsolutePath().toString();
		response.names = names;
		return response;
	}

	private static String resolveConfigName(String kind, ConfigUpsertRequest request, JsonObject cfg) {
		if (request != null && request.name != null && !request.name.trim().isEmpty()) {
			return request.name.trim();
		}
		if ("source".equals(kind) && cfg != null && cfg.has("source") && cfg.get("source").isJsonPrimitive()) {
			String source = cfg.get("source").getAsString();
			if (source != null && !source.trim().isEmpty()) {
				return source.trim();
			}
		}
		return "server".equals(kind) ? "default" : "source";
	}

	private static void validateConfigName(String name) {
		if (name == null || name.trim().isEmpty()) {
			throw new IllegalArgumentException("config name is required");
		}
		if (!SAFE_NAME.matcher(name.trim()).matches()) {
			throw new IllegalArgumentException("invalid config name (allowed: A-Za-z0-9._-): " + name);
		}
	}

	private static void validateConfigShape(String kind, JsonObject cfg) {
		if (cfg == null) {
			throw new IllegalArgumentException("config object is required");
		}
		if ("server".equals(kind)) {
			if (!cfg.has("server") && !cfg.has("setting")) {
				throw new IllegalArgumentException("server config must contain 'server' or 'setting'");
			}
			return;
		}
		if ("source".equals(kind)) {
			if (!cfg.has("source")) {
				throw new IllegalArgumentException("source config must contain 'source'");
			}
			if (!cfg.has("measureV2") && !cfg.has("measure")) {
				throw new IllegalArgumentException("source config must contain 'measureV2' or 'measure'");
			}
		}
	}

	private static Path resolveConfigRoot() {
		String byProperty = System.getProperty("gop.config.path");
		if (byProperty != null && !byProperty.trim().isEmpty()) {
			return Path.of(byProperty.trim()).toAbsolutePath().normalize();
		}
		String byEnv = System.getenv("GOP_CONFIG_PATH");
		if (byEnv != null && !byEnv.trim().isEmpty()) {
			return Path.of(byEnv.trim()).toAbsolutePath().normalize();
		}
		return Path.of("conf").toAbsolutePath().normalize();
	}

	private static Path resolveConfigDir(String kind) {
		Path root = resolveConfigRoot();
		if ("server".equals(kind)) {
			return root.resolve("server").normalize();
		}
		if ("source".equals(kind)) {
			return root.resolve("sources").normalize();
		}
		throw new IllegalArgumentException("invalid kind: " + kind);
	}

	private static String readStatusAsJson(Config config, Gson gson, StatusQuery query) throws IOException {
		List<Data> results = new ArrayList<>();
		List<String> sources = resolveSources(config, query == null ? null : query.source);
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

	private static String readDashboardAsJson(Config config, Gson gson, DashboardQuery query) throws IOException {
		DateTimeFormatter tsFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
		LocalDateTime[] range = resolveTimeRange(tsFormat, query == null ? null : query.timeFrom,
				query == null ? null : query.timeTo);
		LocalDateTime from = range[0];
		LocalDateTime to = range[1];
		String basePath = ConfigManager.resolveLogBasePath(config);
		List<String> sources = resolveSources(config, query == null ? null : query.source);
		Map<String, DashboardAgg> aggMap = new LinkedHashMap<>();
		int badLines = 0;

		DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("yyyyMMdd");
		DateTimeFormatter ymFmt = DateTimeFormatter.ofPattern("yyyy/MM");
		for (String source : sources) {
			validateSourceDir(source);
			String sourceDir = source.trim();
			for (LocalDate d = from.toLocalDate(); !d.isAfter(to.toLocalDate()); d = d.plusDays(1)) {
				String dir = basePath + ymFmt.format(d) + "/" + sourceDir + "/";
				java.io.File logFile = new java.io.File(dir + "log_" + dayFmt.format(d) + ".json");
				java.io.File gzFile = new java.io.File(dir + "log_" + dayFmt.format(d) + ".json.gz");
				if (!logFile.exists() && !gzFile.exists()) {
					continue;
				}
				try (java.io.BufferedReader reader = LogFileUtil.openLogReader(logFile, gzFile)) {
					String line;
					while ((line = reader.readLine()) != null) {
						Data data;
						try {
							data = gson.fromJson(line, Data.class);
						} catch (Exception e) {
							badLines++;
							continue;
						}
						if (data == null || data.time == null || data.rc == null) {
							continue;
						}
						LocalDateTime ts;
						try {
							ts = LocalDateTime.from(tsFormat.parse(data.time));
						} catch (Exception e) {
							badLines++;
							continue;
						}
						if (ts.isBefore(from) || ts.isAfter(to)) {
							continue;
						}
						for (ResultCommon rc : data.rc) {
							if (rc == null) {
								continue;
							}
							if (query != null && query.name != null && !query.name.equals(rc.measure)) {
								continue;
							}
							if (query != null && query.tag != null && !query.tag.equals(rc.tag)) {
								continue;
							}
							String measure = rc.measure == null ? "-" : rc.measure;
							String key = sourceDir + "|" + measure;
							DashboardAgg agg = aggMap.get(key);
							if (agg == null) {
								agg = new DashboardAgg();
								agg.source = sourceDir;
								agg.measure = measure;
								agg.minValue = rc.value;
								agg.maxValue = rc.value;
								aggMap.put(key, agg);
							}
							agg.count++;
							agg.sum += rc.value;
							if (rc.value < agg.minValue) {
								agg.minValue = rc.value;
							}
							if (rc.value > agg.maxValue) {
								agg.maxValue = rc.value;
							}
								if (agg.latestTs == null || agg.latestTs.isBefore(ts)) {
									agg.latestTs = ts;
									agg.latestTime = data.time;
									agg.latestValue = rc.value;
									agg.latestActionStates = rc.actionStates == null ? null : rc.actionStates.clone();
								}
								if (rc.alert) {
									agg.alertCount++;
								}
								if (rc.actionStates != null) {
									agg.actionCount += rc.actionStates.length;
								}
							}
						}
					}
			}
		}
		if (badLines > 0) {
			System.err.println("skip invalid json lines: " + badLines);
		}

		List<DashboardItem> items = new ArrayList<>();
		for (DashboardAgg agg : aggMap.values()) {
			DashboardItem item = new DashboardItem();
			item.source = agg.source;
			item.measure = agg.measure;
			item.latestTime = agg.latestTime;
			item.latestValue = agg.latestValue;
			item.minValue = agg.minValue;
			item.maxValue = agg.maxValue;
				item.count = agg.count;
				item.alertCount = agg.alertCount;
				item.actionCount = agg.actionCount;
				item.latestActionStates = agg.latestActionStates == null ? null : agg.latestActionStates.clone();
				item.avgValue = agg.count == 0 ? 0D : agg.sum / agg.count;
				items.add(item);
			}
		if (query != null && query.limit != null && query.limit > 0 && items.size() > query.limit) {
			items = items.subList(0, query.limit);
		}

		DashboardResponse response = new DashboardResponse();
		response.timeFrom = tsFormat.format(from);
		response.timeTo = tsFormat.format(to);
		response.items = items;
		return gson.toJson(response);
	}

	private static String readDashboardSeriesAsJson(Config config, Gson gson, DashboardSeriesQuery query)
			throws IOException {
		DateTimeFormatter tsFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
		LocalDateTime[] range = resolveTimeRange(tsFormat, query == null ? null : query.timeFrom,
				query == null ? null : query.timeTo);
		LocalDateTime from = range[0];
		LocalDateTime to = range[1];
		String basePath = ConfigManager.resolveLogBasePath(config);
		List<String> sources = resolveSources(config, query == null ? null : query.source);
		Integer limit = query == null ? null : query.limit;
		java.util.Deque<DashboardPoint> tailQueue = limit != null && limit > 0 ? new java.util.LinkedList<>() : null;
		List<DashboardPoint> points = new ArrayList<>();
		int badLines = 0;

		DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("yyyyMMdd");
		DateTimeFormatter ymFmt = DateTimeFormatter.ofPattern("yyyy/MM");
		for (String source : sources) {
			validateSourceDir(source);
			String sourceDir = source.trim();
			for (LocalDate d = from.toLocalDate(); !d.isAfter(to.toLocalDate()); d = d.plusDays(1)) {
				String dir = basePath + ymFmt.format(d) + "/" + sourceDir + "/";
				java.io.File logFile = new java.io.File(dir + "log_" + dayFmt.format(d) + ".json");
				java.io.File gzFile = new java.io.File(dir + "log_" + dayFmt.format(d) + ".json.gz");
				if (!logFile.exists() && !gzFile.exists()) {
					continue;
				}
				try (java.io.BufferedReader reader = LogFileUtil.openLogReader(logFile, gzFile)) {
					String line;
					while ((line = reader.readLine()) != null) {
						Data data;
						try {
							data = gson.fromJson(line, Data.class);
						} catch (Exception e) {
							badLines++;
							continue;
						}
						if (data == null || data.time == null || data.rc == null) {
							continue;
						}
						LocalDateTime ts;
						try {
							ts = LocalDateTime.from(tsFormat.parse(data.time));
						} catch (Exception e) {
							badLines++;
							continue;
						}
						if (ts.isBefore(from) || ts.isAfter(to)) {
							continue;
						}
						for (ResultCommon rc : data.rc) {
							if (rc == null) {
								continue;
							}
							if (query != null && query.name != null && !query.name.equals(rc.measure)) {
								continue;
							}
							if (query != null && query.tag != null && !query.tag.equals(rc.tag)) {
								continue;
							}
							DashboardPoint point = new DashboardPoint();
							point.time = data.time;
							point.source = sourceDir;
							point.measure = rc.measure;
							point.value = rc.value;
							point.tag = rc.tag;
							point.alert = rc.alert;
							point.target = rc.target;
							point.actionStates = rc.actionStates == null ? null : rc.actionStates.clone();
							if (tailQueue != null) {
								tailQueue.addLast(point);
								while (tailQueue.size() > limit) {
									tailQueue.removeFirst();
								}
							} else {
								points.add(point);
							}
						}
					}
				}
			}
		}
		if (badLines > 0) {
			System.err.println("skip invalid json lines: " + badLines);
		}
		if (tailQueue != null) {
			points = new ArrayList<>(tailQueue);
		}
		DashboardSeriesResponse response = new DashboardSeriesResponse();
		response.timeFrom = tsFormat.format(from);
		response.timeTo = tsFormat.format(to);
		response.points = points;
		return gson.toJson(response);
	}

	private static LocalDateTime[] resolveTimeRange(DateTimeFormatter tsFormat, String timeFrom, String timeTo) {
		LocalDateTime from = null;
		LocalDateTime to = null;
		if (timeFrom != null && !timeFrom.isEmpty()) {
			from = LocalDateTime.from(tsFormat.parse(timeFrom));
		}
		if (timeTo != null && !timeTo.isEmpty()) {
			to = LocalDateTime.from(tsFormat.parse(timeTo));
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
		return new LocalDateTime[] { from, to };
	}

	private static List<String> resolveSources(Config config, String sourceFilter) {
		List<String> sources = new ArrayList<>();
		if (sourceFilter != null && !sourceFilter.isEmpty()) {
			sources.add(sourceFilter.trim());
		} else if (config.sources != null && config.sources.length > 0) {
			for (SourceConfig sc : config.sources) {
				String src = sc == null ? null : sc.source;
				if (src == null || src.trim().isEmpty()) {
					src = "default";
				}
				sources.add(src.trim());
			}
		} else {
			SourceConfig sc = buildSingleSource(config);
			String src = sc.source == null || sc.source.trim().isEmpty() ? "default" : sc.source.trim();
			sources.add(src);
		}
		return sources;
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

	private static String dashboardHtml() {
		return """
<!doctype html>
<html lang="ko">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>gop V2 Dashboard</title>
  <style>
    :root {
      --bg-a: #0b132b;
      --bg-b: #1c2541;
      --panel: rgba(11, 19, 43, 0.72);
      --line: rgba(120, 182, 255, 0.26);
      --text: #e9f1ff;
      --muted: #9fb7d9;
      --accent: #4cc9f0;
      --warn: #ffd166;
      --critical: #ff5d73;
      --ok: #80ed99;
    }
    * { box-sizing: border-box; }
    body {
      margin: 0;
      font-family: "IBM Plex Sans", "Noto Sans KR", "Segoe UI", sans-serif;
      color: var(--text);
      background:
        radial-gradient(1000px 600px at 0% 0%, rgba(76, 201, 240, 0.25), transparent 55%),
        radial-gradient(1100px 700px at 100% 0%, rgba(255, 93, 115, 0.2), transparent 60%),
        linear-gradient(160deg, var(--bg-a), var(--bg-b));
      min-height: 100vh;
      padding: 24px;
    }
    .wrap { max-width: 1200px; margin: 0 auto; display: grid; gap: 16px; }
    .panel {
      background: var(--panel);
      border: 1px solid var(--line);
      border-radius: 16px;
      backdrop-filter: blur(8px);
      box-shadow: 0 14px 40px rgba(0, 0, 0, 0.35);
      padding: 16px;
    }
    .title { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
    .title h1 { margin: 0; letter-spacing: 0.04em; font-size: 22px; }
    .subtitle { color: var(--muted); font-size: 13px; }
    .controls {
      display: grid;
      grid-template-columns: repeat(6, minmax(0, 1fr));
      gap: 10px;
      align-items: end;
    }
    .field { display: grid; gap: 6px; }
    .field label { font-size: 12px; color: var(--muted); }
    input, select, button {
      width: 100%;
      border: 1px solid var(--line);
      border-radius: 10px;
      padding: 9px 10px;
      font-size: 13px;
      background: rgba(9, 15, 35, 0.82);
      color: var(--text);
    }
    button {
      cursor: pointer;
      background: linear-gradient(120deg, rgba(76, 201, 240, 0.22), rgba(95, 113, 255, 0.22));
      font-weight: 600;
    }
    .cards { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 10px; }
    .card {
      border: 1px solid var(--line);
      border-radius: 12px;
      padding: 12px;
      background: rgba(5, 10, 26, 0.5);
    }
    .card .k { color: var(--muted); font-size: 12px; }
    .card .v { font-size: 20px; margin-top: 4px; font-weight: 700; }
    .grid { display: grid; grid-template-columns: 1.2fr 1fr; gap: 12px; }
    .config-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
    table { width: 100%; border-collapse: collapse; font-size: 12px; }
    th, td { border-bottom: 1px solid rgba(130, 176, 240, 0.18); padding: 8px 6px; text-align: left; }
    th { color: var(--muted); font-weight: 600; }
    tr:hover { background: rgba(76, 201, 240, 0.09); }
    .pill {
      display: inline-block;
      padding: 2px 7px;
      border-radius: 999px;
      border: 1px solid var(--line);
      font-size: 11px;
      color: var(--muted);
    }
    .critical { color: var(--critical); border-color: rgba(255, 93, 115, 0.35); }
    .warn { color: var(--warn); border-color: rgba(255, 209, 102, 0.35); }
    .ok { color: var(--ok); border-color: rgba(128, 237, 153, 0.35); }
    canvas {
      width: 100%;
      height: 260px;
      border: 1px solid var(--line);
      border-radius: 12px;
      background: rgba(6, 12, 30, 0.55);
    }
    textarea {
      width: 100%;
      min-height: 180px;
      border: 1px solid var(--line);
      border-radius: 10px;
      padding: 9px 10px;
      font-size: 12px;
      background: rgba(9, 15, 35, 0.82);
      color: var(--text);
      font-family: "IBM Plex Mono", "Menlo", monospace;
      resize: vertical;
    }
    .mini {
      margin-top: 8px;
      padding: 8px 10px;
      border: 1px solid var(--line);
      border-radius: 10px;
      background: rgba(6, 12, 30, 0.5);
      color: var(--muted);
      font-size: 12px;
      min-height: 38px;
      white-space: pre-wrap;
    }
    .row2 { display: grid; grid-template-columns: 1fr auto; gap: 8px; }
    .hint { color: var(--muted); font-size: 12px; margin-top: 6px; }
    @media (max-width: 980px) {
      .controls { grid-template-columns: repeat(2, minmax(0, 1fr)); }
      .cards { grid-template-columns: repeat(2, minmax(0, 1fr)); }
      .config-grid { grid-template-columns: 1fr; }
      .grid { grid-template-columns: 1fr; }
    }
  </style>
</head>
<body>
  <div class="wrap">
    <div class="panel">
      <div class="title">
        <h1>gop V2 Dashboard</h1>
        <span class="subtitle" id="updatedAt">-</span>
      </div>
      <div class="controls" style="margin-top: 12px;">
        <div class="field"><label>Source</label><input id="source" placeholder="(all)" /></div>
        <div class="field"><label>Measure</label><input id="name" placeholder="(all)" /></div>
        <div class="field"><label>Tag</label><input id="tag" placeholder="(all)" /></div>
        <div class="field"><label>From</label><input id="timeFrom" type="datetime-local" /></div>
        <div class="field"><label>To</label><input id="timeTo" type="datetime-local" /></div>
        <div class="field"><label>Refresh(sec)</label><select id="refresh"><option>0</option><option>5</option><option>10</option><option>30</option></select></div>
        <div class="field"><label>Series source</label><select id="seriesSource"><option value="">(auto)</option></select></div>
        <div class="field"><label>Series measure</label><select id="seriesMeasure"><option value="">(auto)</option></select></div>
        <div class="field"><label>Series points</label><input id="limit" type="number" value="200" min="10" max="5000" /></div>
        <div class="field"><label>&nbsp;</label><button id="reloadBtn">Reload</button></div>
      </div>
    </div>

    <div class="cards">
      <div class="card"><div class="k">Metrics</div><div class="v" id="metricsCount">0</div></div>
      <div class="card"><div class="k">Alerts</div><div class="v" id="alertsCount">0</div></div>
      <div class="card"><div class="k">Actions</div><div class="v" id="actionsCount">0</div></div>
      <div class="card"><div class="k">Series points</div><div class="v" id="pointsCount">0</div></div>
    </div>

    <div class="config-grid">
      <div class="panel">
        <div class="title"><h1 style="font-size:16px">Server Config</h1><span class="subtitle" id="serverListInfo">-</span></div>
        <div class="row2" style="margin-top:10px;">
          <input id="serverCfgName" placeholder="name (default: default)" />
          <button id="saveServerBtn">Save Server</button>
        </div>
        <textarea id="serverCfgText" placeholder='{"schemaVersion":2,"server":{"timeInterval":1000,"consolePrint":true,"sourceRefs":["mysql-local"]}}'></textarea>
        <div class="mini" id="serverCfgMsg">ready</div>
      </div>
      <div class="panel">
        <div class="title"><h1 style="font-size:16px">Source Config</h1><span class="subtitle" id="sourceListInfo">-</span></div>
        <div class="row2" style="margin-top:10px;">
          <input id="sourceCfgName" placeholder="name (optional, fallback to source)" />
          <button id="saveSourceBtn">Save Source</button>
        </div>
        <textarea id="sourceCfgText" placeholder='{"schemaVersion":2,"source":"mysql-local","measureV2":[]}'></textarea>
        <div class="mini" id="sourceCfgMsg">ready</div>
      </div>
    </div>

    <div class="grid">
      <div class="panel">
        <table>
          <thead>
            <tr>
              <th>source</th><th>measure</th><th>latest</th><th>min</th><th>max</th><th>avg</th><th>alerts</th><th>actions</th>
            </tr>
          </thead>
          <tbody id="tableBody"></tbody>
        </table>
        <div class="hint">row 클릭 시 우측 차트 필터가 자동으로 맞춰집니다.</div>
      </div>
      <div class="panel">
        <canvas id="seriesCanvas" width="560" height="300"></canvas>
        <div class="hint" id="chartHint">no data</div>
      </div>
    </div>
  </div>

  <script>
    const $ = (id) => document.getElementById(id);
    const state = { summary: null, series: null, timer: null };

    function toApiTimestamp(v) {
      if (!v) return null;
      const x = v.replace("T", " ");
      return x.length === 16 ? x + ":00.000" : x + ".000";
    }

    function queryBase() {
      const q = {};
      const source = $("source").value.trim();
      const name = $("name").value.trim();
      const tag = $("tag").value.trim();
      const from = toApiTimestamp($("timeFrom").value.trim());
      const to = toApiTimestamp($("timeTo").value.trim());
      if (source) q.source = source;
      if (name) q.name = name;
      if (tag) q.tag = tag;
      if (from) q.timeFrom = from;
      if (to) q.timeTo = to;
      return q;
    }

    async function postJson(path, payload) {
      const r = await fetch(path, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload || {})
      });
      if (!r.ok) {
        const t = await r.text();
        throw new Error(path + " " + r.status + " " + t);
      }
      return await r.json();
    }

    async function getJson(path) {
      const r = await fetch(path, { method: "GET" });
      if (!r.ok) {
        const t = await r.text();
        throw new Error(path + " " + r.status + " " + t);
      }
      return await r.json();
    }

    function fmtNum(v) {
      if (v === null || v === undefined) return "-";
      if (typeof v === "number") return Number.isInteger(v) ? String(v) : v.toFixed(2);
      return String(v);
    }

    function badge(alertCount) {
      if (alertCount > 0) return '<span class="pill critical">critical</span>';
      return '<span class="pill ok">ok</span>';
    }

    function renderCards() {
      const items = (state.summary && state.summary.items) || [];
      const points = (state.series && state.series.points) || [];
      let alerts = 0;
      let actions = 0;
      for (const x of items) {
        alerts += x.alertCount || 0;
        actions += x.actionCount || 0;
      }
      $("metricsCount").textContent = String(items.length);
      $("alertsCount").textContent = String(alerts);
      $("actionsCount").textContent = String(actions);
      $("pointsCount").textContent = String(points.length);
    }

    function renderTable() {
      const items = (state.summary && state.summary.items) || [];
      const body = $("tableBody");
      if (items.length === 0) {
        body.innerHTML = '<tr><td colspan="8">no data</td></tr>';
        return;
      }
      body.innerHTML = items.map((x) => `
        <tr data-source="${x.source || ""}" data-measure="${x.measure || ""}">
          <td>${x.source || "-"}</td>
          <td>${x.measure || "-"}</td>
          <td>${fmtNum(x.latestValue)}</td>
          <td>${fmtNum(x.minValue)}</td>
          <td>${fmtNum(x.maxValue)}</td>
          <td>${fmtNum(x.avgValue)}</td>
          <td>${fmtNum(x.alertCount)} ${badge(x.alertCount || 0)}</td>
          <td>${fmtNum(x.actionCount)}</td>
        </tr>
      `).join("");
      for (const tr of body.querySelectorAll("tr[data-source]")) {
        tr.addEventListener("click", () => {
          $("seriesSource").value = tr.dataset.source || "";
          $("seriesMeasure").value = tr.dataset.measure || "";
          renderChart();
        });
      }
    }

    function setMsg(id, text, isError) {
      const el = $(id);
      el.textContent = text;
      el.style.borderColor = isError ? "rgba(255, 93, 115, 0.55)" : "var(--line)";
      el.style.color = isError ? "var(--critical)" : "var(--muted)";
    }

    function prettyJsonText(raw) {
      try {
        return JSON.stringify(JSON.parse(raw), null, 2);
      } catch (_) {
        return raw;
      }
    }

    function buildDefaultServerConfig() {
      return {
        schemaVersion: 2,
        server: {
          timeInterval: 1000,
          consolePrint: true,
          pageSize: "10",
          retention: "2",
          printCSV: false,
          fileLog: { enable: true, logPath: "data/" },
          api: { enable: true, port: 18080, threadPoolSize: 4, logPath: "data/api.log" },
          sourceRefs: ["mysql-local"]
        }
      };
    }

    function buildDefaultSourceConfig() {
      return {
        schemaVersion: 2,
        source: "mysql-local",
        measureV2: []
      };
    }

    async function loadConfigLists() {
      const [servers, sources] = await Promise.all([
        getJson("/config/server"),
        getJson("/config/source")
      ]);
      $("serverListInfo").textContent = (servers.names || []).join(", ") || "(none)";
      $("sourceListInfo").textContent = (sources.names || []).join(", ") || "(none)";
    }

    async function saveConfig(kind) {
      const isServer = kind === "server";
      const nameId = isServer ? "serverCfgName" : "sourceCfgName";
      const textId = isServer ? "serverCfgText" : "sourceCfgText";
      const msgId = isServer ? "serverCfgMsg" : "sourceCfgMsg";
      const name = $(nameId).value.trim();
      const raw = $(textId).value.trim();
      if (!raw) {
        setMsg(msgId, "json 본문을 입력하세요", true);
        return;
      }
      let parsed;
      try {
        parsed = JSON.parse(raw);
      } catch (e) {
        setMsg(msgId, "json parse error: " + e.message, true);
        return;
      }
      const payload = { config: parsed };
      if (name) payload.name = name;
      try {
        const resp = await postJson("/config/" + kind, payload);
        $(textId).value = prettyJsonText(JSON.stringify(parsed));
        setMsg(msgId, "saved: " + resp.path, false);
        await loadConfigLists();
      } catch (e) {
        setMsg(msgId, String(e.message || e), true);
      }
    }

    function ensureSelectOptions(id, values) {
      const sel = $(id);
      const current = sel.value;
      const list = ["", ...Array.from(new Set(values)).sort()];
      sel.innerHTML = list.map((v) => `<option value="${v}">${v || "(auto)"}</option>`).join("");
      if (list.includes(current)) sel.value = current;
    }

    function renderChart() {
      const canvas = $("seriesCanvas");
      const ctx = canvas.getContext("2d");
      const all = (state.series && state.series.points) || [];
      const selectedSource = $("seriesSource").value;
      const selectedMeasure = $("seriesMeasure").value;
      const points = all.filter((p) =>
        (!selectedSource || p.source === selectedSource) &&
        (!selectedMeasure || p.measure === selectedMeasure)
      );
      ctx.clearRect(0, 0, canvas.width, canvas.height);
      ctx.fillStyle = "rgba(130, 176, 240, 0.2)";
      ctx.fillRect(0, 0, canvas.width, canvas.height);
      if (points.length < 2) {
        $("chartHint").textContent = "not enough points";
        return;
      }

      let min = Number.POSITIVE_INFINITY;
      let max = Number.NEGATIVE_INFINITY;
      for (const p of points) {
        if (p.value < min) min = p.value;
        if (p.value > max) max = p.value;
      }
      if (min === max) { min -= 1; max += 1; }

      const padL = 40, padR = 14, padT = 16, padB = 30;
      const w = canvas.width - padL - padR;
      const h = canvas.height - padT - padB;

      ctx.strokeStyle = "rgba(170, 210, 255, 0.25)";
      ctx.lineWidth = 1;
      for (let i = 0; i <= 4; i++) {
        const y = padT + (h * i / 4);
        ctx.beginPath(); ctx.moveTo(padL, y); ctx.lineTo(padL + w, y); ctx.stroke();
      }

      ctx.strokeStyle = "#4cc9f0";
      ctx.lineWidth = 2;
      ctx.beginPath();
      points.forEach((p, i) => {
        const x = padL + (w * i / (points.length - 1));
        const y = padT + (h * (max - p.value) / (max - min));
        if (i === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
      });
      ctx.stroke();

      const last = points[points.length - 1];
      const label = `${last.source || "-"} / ${last.measure || "-"}  latest=${last.value}`;
      $("chartHint").textContent = label + `  range[${fmtNum(min)} .. ${fmtNum(max)}]`;
    }

    async function loadAll() {
      const q = queryBase();
      const limit = Math.max(10, Number($("limit").value || "200"));
      const [summary, series] = await Promise.all([
        postJson("/dashboard", q),
        postJson("/dashboard/series", { ...q, limit })
      ]);
      state.summary = summary;
      state.series = series;
      renderCards();
      renderTable();
      const srcValues = (series.points || []).map((x) => x.source || "");
      const msValues = (series.points || []).map((x) => x.measure || "");
      ensureSelectOptions("seriesSource", srcValues);
      ensureSelectOptions("seriesMeasure", msValues);
      renderChart();
      $("updatedAt").textContent = "updated " + new Date().toLocaleString();
    }

    function setupAutoRefresh() {
      if (state.timer) {
        clearInterval(state.timer);
        state.timer = null;
      }
      const sec = Number($("refresh").value || "0");
      if (sec > 0) {
        state.timer = setInterval(() => { loadAll().catch(console.error); }, sec * 1000);
      }
    }

    function bootstrap() {
      const now = new Date();
      const from = new Date(now.getFullYear(), now.getMonth(), now.getDate());
      const toLocal = (d) => new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
      $("timeFrom").value = toLocal(from);
      $("timeTo").value = toLocal(now);
      $("serverCfgText").value = JSON.stringify(buildDefaultServerConfig(), null, 2);
      $("sourceCfgText").value = JSON.stringify(buildDefaultSourceConfig(), null, 2);
      $("reloadBtn").addEventListener("click", () => loadAll().catch(console.error));
      $("saveServerBtn").addEventListener("click", () => saveConfig("server"));
      $("saveSourceBtn").addEventListener("click", () => saveConfig("source"));
      $("refresh").addEventListener("change", setupAutoRefresh);
      $("seriesSource").addEventListener("change", renderChart);
      $("seriesMeasure").addEventListener("change", renderChart);
      loadAll().catch((e) => { $("updatedAt").textContent = e.message; });
      loadConfigLists().catch((e) => {
        $("serverListInfo").textContent = "load failed";
        $("sourceListInfo").textContent = String(e.message || e);
      });
      setupAutoRefresh();
    }
    bootstrap();
  </script>
</body>
</html>
""";
	}

	private static void validateSourceDir(String source) {
		ConfigManager.validateId("source id", source);
	}

	private static SourceConfig buildSingleSource(Config config) {
		SourceConfig source = new SourceConfig();
		source.source = config.setting.source == null ? "default" : config.setting.source;
		source.jdbcSource = config.setting.jdbcSource;
		source.measure = config.measure;
		source.measureV2 = config.measureV2;
		return source;
	}
}

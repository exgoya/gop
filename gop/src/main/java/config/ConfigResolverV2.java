package config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

import model.Config;
import model.MeasureV2;
import model.SourceConfig;

public final class ConfigResolverV2 {
	private static final int REQUIRED_SCHEMA_VERSION = 2;

	private ConfigResolverV2() {
	}

	public static class ValidationResult {
		public boolean valid;
		public String serverPath;
		public List<String> sourcePaths = new ArrayList<>();
		public List<String> errors = new ArrayList<>();
		public List<String> warnings = new ArrayList<>();
	}

	public static ValidationResult validateServerConfig(File serverFile, Gson gson) {
		ValidationResult result = new ValidationResult();
		result.serverPath = serverFile == null ? null : serverFile.getAbsolutePath();
		try {
			Config config = loadServerConfig(serverFile, gson);
			if (config.sources != null) {
				for (SourceConfig source : config.sources) {
					if (source != null && source.configPath != null && !source.configPath.trim().isEmpty()) {
						result.sourcePaths.add(source.configPath);
					}
				}
			}
		} catch (Exception e) {
			result.errors.add(e.getMessage() == null ? e.toString() : e.getMessage());
		}
		result.valid = result.errors.isEmpty();
		return result;
	}

	public static Config loadServerConfig(File serverFile, Gson gson) throws IOException {
		if (serverFile == null) {
			throw new IllegalArgumentException("server config path is required");
		}
		if (!serverFile.exists()) {
			throw new IllegalArgumentException("server config not found: " + serverFile.getPath());
		}
		Config config = readConfig(serverFile, gson);
		validateServerSchema(config, serverFile);

		if (config.measure != null && config.measure.length > 0) {
			throw new IllegalArgumentException(
					"legacy field 'measure' is not supported in V2. migrate to source config 'measureV2'.");
		}
		if (config.setting == null && config.server != null) {
			config.setting = config.server;
		}
		if (config.setting == null) {
			throw new IllegalArgumentException("invalid config: missing 'server' section");
		}

		if ((config.sources == null || config.sources.length == 0)
				&& config.server != null && config.server.sourceRefs != null && config.server.sourceRefs.length > 0) {
			config.sources = loadSourceConfigs(serverFile, config.server.sourceRefs, gson);
		} else if (config.sources != null) {
			for (SourceConfig source : config.sources) {
				validateSourceConfig(source, null);
			}
		}

		ConfigManager.ensureConfigId(config, serverFile);
		config.setting.runtimeConfigPath = serverFile.getAbsolutePath();
		return config;
	}

	private static Config readConfig(File configFile, Gson gson) throws IOException {
		try (FileInputStream fis = new FileInputStream(configFile);
				InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
				BufferedReader reader = new BufferedReader(isr)) {
			Config config = gson.fromJson(reader, Config.class);
			if (config == null) {
				throw new IllegalArgumentException("invalid config file: empty json (" + configFile.getPath() + ")");
			}
			return config;
		}
	}

	private static void validateServerSchema(Config config, File serverFile) {
		if (config.schemaVersion == null) {
			throw new IllegalArgumentException("schemaVersion is required in server config: " + serverFile.getPath());
		}
		if (config.schemaVersion.intValue() != REQUIRED_SCHEMA_VERSION) {
			throw new IllegalArgumentException("unsupported schemaVersion: " + config.schemaVersion
					+ " (expected " + REQUIRED_SCHEMA_VERSION + ") in " + serverFile.getPath());
		}
	}

	private static SourceConfig[] loadSourceConfigs(File serverFile, String[] refs, Gson gson)
			throws IOException {
		List<SourceConfig> sources = new ArrayList<>();
		for (String ref : refs) {
			if (ref == null || ref.trim().isEmpty()) {
				continue;
			}
			File sourceFile = resolveSourceFile(serverFile, ref.trim());
			if (sourceFile == null || !sourceFile.exists()) {
				throw new IllegalArgumentException("source config not found: " + ref);
			}
			SourceConfig source = readSourceConfig(sourceFile, gson);
			if (source.source == null || source.source.trim().isEmpty()) {
				source.source = sourceFile.getName().replaceFirst("\\.json$", "");
			}
			source.configPath = sourceFile.getAbsolutePath();
			validateSourceConfig(source, sourceFile);
			sources.add(source);
		}
		return sources.toArray(new SourceConfig[0]);
	}

	private static SourceConfig readSourceConfig(File sourceFile, Gson gson) throws IOException {
		try (FileInputStream fis = new FileInputStream(sourceFile);
				InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
				BufferedReader reader = new BufferedReader(isr)) {
			SourceConfig source = gson.fromJson(reader, SourceConfig.class);
			if (source == null) {
				throw new IllegalArgumentException("invalid source config: " + sourceFile.getPath());
			}
			return source;
		}
	}

	private static void validateSourceConfig(SourceConfig source, File sourceFile) {
		String label = sourceFile == null ? firstNonBlank(source == null ? null : source.source, "inline-source")
				: sourceFile.getPath();
		if (source == null) {
			throw new IllegalArgumentException("invalid source config: " + label);
		}
		if (source.schemaVersion == null) {
			throw new IllegalArgumentException("schemaVersion is required in source config: " + label);
		}
		if (source.schemaVersion.intValue() != REQUIRED_SCHEMA_VERSION) {
			throw new IllegalArgumentException(
					"unsupported source schemaVersion: " + source.schemaVersion + " (expected " + REQUIRED_SCHEMA_VERSION + ") in " + label);
		}
		if (source.measure != null && source.measure.length > 0) {
			throw new IllegalArgumentException(
					"legacy field 'measure' is not supported in source config. use 'measureV2' instead: " + label);
		}
		if (source.measureV2 == null || source.measureV2.length == 0) {
			throw new IllegalArgumentException("source config requires non-empty 'measureV2': " + label);
		}
		if (source.source == null || source.source.trim().isEmpty()) {
			throw new IllegalArgumentException("source id is required in source config: " + label);
		}
		for (MeasureV2 measure : source.measureV2) {
			if (measure == null) {
				throw new IllegalArgumentException("null measureV2 entry is not allowed: " + label);
			}
			if (measure.sql == null || measure.sql.trim().isEmpty()) {
				throw new IllegalArgumentException("measureV2.sql is required for " + source.source + " (" + label + ")");
			}
			if (!measure.sqlIsOs && source.jdbcSource == null) {
				throw new IllegalArgumentException(
						"jdbcSource is required for SQL measureV2 in source " + source.source + " (" + label + ")");
			}
		}
	}

	private static File resolveSourceFile(File serverFile, String sourceRef) {
		File refFile = new File(sourceRef);
		if (refFile.isAbsolute()) {
			return refFile;
		}
		File baseDir = serverFile.getParentFile();
		List<File> candidates = new ArrayList<>();
		if (sourceRef.contains("/") || sourceRef.contains("\\") || sourceRef.endsWith(".json")) {
			candidates.add(new File(baseDir, sourceRef));
		} else {
			candidates.add(new File(baseDir, sourceRef + ".json"));
		}
		File parent = baseDir == null ? null : baseDir.getParentFile();
		if (parent != null) {
			if (sourceRef.endsWith(".json")) {
				candidates.add(new File(parent, "sources/" + sourceRef));
			} else {
				candidates.add(new File(parent, "sources/" + sourceRef + ".json"));
			}
		}
		for (File candidate : candidates) {
			if (candidate.exists()) {
				return candidate;
			}
		}
		return candidates.isEmpty() ? null : candidates.get(0);
	}

	private static String firstNonBlank(String... values) {
		if (values == null) {
			return null;
		}
		for (String value : values) {
			if (value != null && !value.trim().isEmpty()) {
				return value.trim();
			}
		}
		return null;
	}
}

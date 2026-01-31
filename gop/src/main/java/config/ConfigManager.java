package config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import model.Config;

public final class ConfigManager {
	private static final String ID_PATTERN = "[A-Za-z0-9._-]+";

	private ConfigManager() {
	}

	public static void ensureConfigId(Config config, File configFile) {
		if (config == null || config.setting == null) {
			return;
		}
		if (config.setting.configId != null && !config.setting.configId.trim().isEmpty()) {
			validateId("config id", config.setting.configId);
			return;
		}
		String name = configFile == null ? "config" : configFile.getName();
		int dot = name.lastIndexOf('.');
		if (dot > 0) {
			name = name.substring(0, dot);
		}
		String safe = toSafeId(name);
		config.setting.configId = safe.isEmpty() ? "config" : safe;
	}

	public static String resolveLogBasePath(Config config) {
		String basePath = config == null || config.setting == null || config.setting.fileLog == null
				? ""
				: config.setting.fileLog.logPath;
		if (basePath == null) {
			basePath = "";
		}
		if (!basePath.endsWith("/")) {
			basePath = basePath + "/";
		}
		String configId = config == null || config.setting == null ? null : config.setting.configId;
		if (configId != null && !configId.trim().isEmpty()) {
			validateId("config id", configId);
			basePath = basePath + configId.trim() + "/";
		}
		return basePath;
	}

	public static void ensureConfigFolderConsistency(Config config, File configFile) {
		if (config == null || config.setting == null || config.setting.fileLog == null) {
			return;
		}
		if (!config.setting.fileLog.enable) {
			return;
		}
		if (configFile == null || !configFile.exists()) {
			return;
		}
		String basePath = resolveLogBasePath(config);
		File baseDir = new File(basePath);
		File targetConfig = new File(baseDir, "config.json");
		if (targetConfig.exists()) {
			try {
				if (!sameFileContent(configFile, targetConfig)) {
					File backup = backupConfigFolder(baseDir);
					System.out.println("config changed, backup: " + backup.getAbsolutePath());
				}
			} catch (IOException e) {
				System.err.println("config compare failed: " + e.getMessage());
				System.exit(0);
			}
		}
		copyConfigToLogBase(config, configFile);
	}

	private static void copyConfigToLogBase(Config config, File configFile) {
		if (config == null || config.setting == null || config.setting.fileLog == null) {
			return;
		}
		if (!config.setting.fileLog.enable) {
			return;
		}
		if (configFile == null || !configFile.exists()) {
			return;
		}
		String basePath = resolveLogBasePath(config);
		File dir = new File(basePath);
		if (!dir.exists()) {
			dir.mkdirs();
		}
		File target = new File(dir, "config.json");
		try {
			Files.copy(configFile.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			System.err.println("failed to copy config: " + e.getMessage());
		}
	}

	private static boolean sameFileContent(File a, File b) throws IOException {
		byte[] aBytes = Files.readAllBytes(a.toPath());
		byte[] bBytes = Files.readAllBytes(b.toPath());
		if (aBytes.length != bBytes.length) {
			return false;
		}
		for (int i = 0; i < aBytes.length; i++) {
			if (aBytes[i] != bBytes[i]) {
				return false;
			}
		}
		return true;
	}

	private static File backupConfigFolder(File baseDir) throws IOException {
		if (baseDir == null || !baseDir.exists()) {
			return baseDir;
		}
		String ts = java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
				.format(java.time.LocalDateTime.now());
		File parent = baseDir.getParentFile();
		String name = baseDir.getName();
		File backup = new File(parent, name + "_old_" + ts);
		Files.move(baseDir.toPath(), backup.toPath(), StandardCopyOption.ATOMIC_MOVE);
		baseDir.mkdirs();
		return backup;
	}

	public static void validateId(String label, String id) {
		if (id == null || id.trim().isEmpty()) {
			return;
		}
		String trimmed = id.trim();
		if (!trimmed.matches(ID_PATTERN)) {
			System.out.println("invalid " + label + " (allowed: A-Za-z0-9._-): " + id);
			System.exit(0);
		}
	}

	private static String toSafeId(String raw) {
		if (raw == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < raw.length(); i++) {
			char c = raw.charAt(i);
			if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '.' || c == '_' || c == '-') {
				sb.append(c);
			} else {
				sb.append('_');
			}
		}
		return sb.toString();
	}
}

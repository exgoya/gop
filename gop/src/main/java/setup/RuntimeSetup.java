package setup;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import model.Config;

public final class RuntimeSetup {
    private static final String DEFAULT_BIN = "/opt/gop/bin/gop";
    private static final String DEFAULT_LINK = "/usr/local/bin/gop";
    private static final String DEFAULT_CONFIG_SRC = "/opt/gop/config";
    private static final String DEFAULT_CONFIG_DST = "/etc/gop";
    private static final String DEFAULT_DATA_DIR = "/var/lib/gop";
    private static final String DEFAULT_LOG_DIR = "/var/log/gop";

    private RuntimeSetup() {
    }

    public static void runInit(boolean verbose) {
        if (!isLinux()) {
            System.out.println("init is supported on Linux only.");
            return;
        }
        if (!isRoot()) {
            System.out.println("init requires root. run: sudo gop init");
            return;
        }
        initializeSystemPaths(verbose);
    }

    public static void autoInit(Config config, File configFile) {
        if (!isLinux()) {
            return;
        }
        if (!isRoot()) {
            if (usesSystemPaths(config, configFile)) {
                System.out.println("warning: system paths require root. run: sudo gop init");
            }
            return;
        }
        initializeSystemPaths(false);
    }

    private static void initializeSystemPaths(boolean verbose) {
        ensureDir(Paths.get(DEFAULT_CONFIG_DST), verbose);
        ensureDir(Paths.get(DEFAULT_DATA_DIR), verbose);
        ensureDir(Paths.get(DEFAULT_LOG_DIR), verbose);
        copyConfigSamples(Paths.get(DEFAULT_CONFIG_SRC), Paths.get(DEFAULT_CONFIG_DST), verbose);
        ensureSymlink(Paths.get(DEFAULT_LINK), Paths.get(DEFAULT_BIN), verbose);
    }

    private static void ensureDir(Path path, boolean verbose) {
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                if (verbose) {
                    System.out.println("created: " + path);
                }
            }
        } catch (IOException e) {
            if (verbose) {
                System.out.println("failed to create: " + path + " (" + e.getMessage() + ")");
            }
        }
    }

    private static void copyConfigSamples(Path srcDir, Path dstDir, boolean verbose) {
        if (!Files.isDirectory(srcDir)) {
            return;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(srcDir, "*.json")) {
            for (Path src : stream) {
                Path dst = dstDir.resolve(src.getFileName().toString());
                if (Files.exists(dst)) {
                    continue;
                }
                try {
                    Files.copy(src, dst);
                    if (verbose) {
                        System.out.println("copied: " + dst);
                    }
                } catch (IOException e) {
                    if (verbose) {
                        System.out.println("failed to copy: " + src + " (" + e.getMessage() + ")");
                    }
                }
            }
        } catch (IOException e) {
            if (verbose) {
                System.out.println("failed to read config samples: " + e.getMessage());
            }
        }
    }

    private static void ensureSymlink(Path link, Path target, boolean verbose) {
        if (!Files.exists(target)) {
            return;
        }
        try {
            if (Files.exists(link)) {
                if (Files.isSymbolicLink(link)) {
                    Path current = Files.readSymbolicLink(link);
                    if (current.equals(target)) {
                        return;
                    }
                }
                if (verbose) {
                    System.out.println("skip existing: " + link);
                }
                return;
            }
            Path parent = link.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.createSymbolicLink(link, target);
            if (verbose) {
                System.out.println("linked: " + link + " -> " + target);
            }
        } catch (IOException e) {
            if (verbose) {
                System.out.println("failed to link: " + link + " (" + e.getMessage() + ")");
            }
        }
    }

    private static boolean usesSystemPaths(Config config, File configFile) {
        if (configFile != null) {
            String path = configFile.getAbsolutePath();
            if (path.startsWith(DEFAULT_CONFIG_DST + File.separator)) {
                return true;
            }
        }
        if (config == null || config.setting == null) {
            return false;
        }
        if (config.setting.fileLog != null && config.setting.fileLog.logPath != null) {
            if (config.setting.fileLog.logPath.startsWith("/var/")) {
                return true;
            }
        }
        if (config.setting.api != null && config.setting.api.logPath != null) {
            if (config.setting.api.logPath.startsWith("/var/")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isLinux() {
        String os = System.getProperty("os.name");
        return os != null && os.toLowerCase().contains("linux");
    }

    private static boolean isRoot() {
        String user = System.getProperty("user.name");
        if ("root".equals(user)) {
            return true;
        }
        try {
            Process process = new ProcessBuilder("id", "-u").start();
            byte[] out = process.getInputStream().readAllBytes();
            process.waitFor();
            String result = new String(out).trim();
            return "0".equals(result);
        } catch (Exception e) {
            return false;
        }
    }
}

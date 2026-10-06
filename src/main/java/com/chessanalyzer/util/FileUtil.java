package com.chessanalyzer.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class FileUtil {

    private FileUtil() {
    }

    public static String readTextFile(File file) throws IOException {
        return Files.readString(file.toPath(), StandardCharsets.UTF_8);
    }

    public static void writeTextFile(File file, String content) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
    }

    /**
     * Tries a handful of common install locations / PATH lookups for the
     * Stockfish executable. Returns null if nothing is found; the caller
     * should then prompt the user to configure the path manually.
     */
    public static String tryAutoDetectStockfish() {
        String os = System.getProperty("os.name", "").toLowerCase();
        List<String> candidates;

        if (os.contains("win")) {
            candidates = List.of(
                    "C:\\stockfish\\stockfish.exe",
                    "C:\\Program Files\\Stockfish\\stockfish.exe",
                    "stockfish.exe"
            );
        } else if (os.contains("mac")) {
            candidates = List.of(
                    "/opt/homebrew/bin/stockfish",
                    "/usr/local/bin/stockfish",
                    "/usr/bin/stockfish"
            );
        } else {
            candidates = List.of(
                    "/usr/games/stockfish",
                    "/usr/bin/stockfish",
                    "/usr/local/bin/stockfish"
            );
        }

        for (String candidate : candidates) {
            File f = new File(candidate);
            if (f.exists() && f.canExecute()) {
                return f.getAbsolutePath();
            }
        }

        // Fall back to searching PATH for a bare "stockfish" / "stockfish.exe" entry.
        String pathEnv = System.getenv("PATH");
        if (pathEnv != null) {
            String exeName = os.contains("win") ? "stockfish.exe" : "stockfish";
            for (String dir : pathEnv.split(File.pathSeparator)) {
                File f = new File(dir, exeName);
                if (f.exists() && f.canExecute()) {
                    return f.getAbsolutePath();
                }
            }
        }

        return null;
    }

    public static boolean isValidExecutable(String path) {
        if (path == null || path.isBlank()) return false;
        File f = new File(path);
        return f.exists() && f.isFile() && f.canExecute();
    }

    public static Path ensureDataDir() {
        Path dir = Path.of("data", "analyses");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create data directory: " + dir, e);
        }
        return dir;
    }
}

package com.chessanalyzer.service;

import com.chessanalyzer.model.AnalysisSettings;
import com.chessanalyzer.model.Evaluation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Owns a single Stockfish UCI process for the lifetime of one analysis run.
 * Not thread-safe: intended to be driven from a single background thread
 * (see AnalysisService, which runs inside a JavaFX Task).
 *
 * Keep all Stockfish process/UCI plumbing here -- never in a controller.
 */
public class StockfishService implements AutoCloseable {

    private Process process;
    private BufferedReader reader;
    private Writer writer;
    private volatile boolean running = false;

    /**
     * Launches the engine, performs the UCI handshake, and applies engine options.
     */
    public void start(AnalysisSettings settings) throws EngineException {
        String path = settings.getStockfishPath();
        if (path == null || path.isBlank()) {
            throw new EngineException("No Stockfish executable path is configured.");
        }
        try {
            ProcessBuilder pb = new ProcessBuilder(path);
            pb.redirectErrorStream(true);
            process = pb.start();
            reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            writer = new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8);

            send("uci");
            waitFor("uciok", 10_000);

            send("setoption name Threads value " + settings.getEngineThreads());
            send("setoption name Hash value " + settings.getEngineHashMb());

            send("isready");
            waitFor("readyok", 10_000);

            send("ucinewgame");
            send("isready");
            waitFor("readyok", 10_000);

            running = true;
        } catch (IOException e) {
            throw new EngineException("Could not start the Stockfish engine at: " + path
                    + ". Check that the path is correct and the file is executable.", e);
        }
    }

    public boolean isRunning() {
        return running && process != null && process.isAlive();
    }

    /**
     * Analyzes one position to a fixed depth, with a wall-clock safety cap.
     * If {@code cancelFlag} becomes true while the engine is thinking, the
     * search is stopped early and whatever best move was found is returned.
     */
    public EngineAnalysisResult analyzePosition(String fen, boolean whiteToMove, int depth,
                                                 int moveTimeLimitMs, AtomicBoolean cancelFlag) throws EngineException {
        if (!isRunning()) {
            throw new EngineException("Stockfish is not running.");
        }
        try {
            send("position fen " + fen);
            send("go depth " + depth);

            // Safety-net thread: force a stop if the engine runs longer than the cap,
            // or if the caller requested cancellation.
            Thread watchdog = new Thread(() -> {
                long deadline = System.currentTimeMillis() + moveTimeLimitMs;
                try {
                    while (System.currentTimeMillis() < deadline) {
                        if (cancelFlag != null && cancelFlag.get()) {
                            send("stop");
                            return;
                        }
                        Thread.sleep(50);
                    }
                    send("stop");
                } catch (InterruptedException ignored) {
                    // normal shutdown
                } catch (IOException ignored) {
                    // engine already gone
                }
            }, "stockfish-watchdog");
            watchdog.setDaemon(true);
            watchdog.start();

            Evaluation lastEval = Evaluation.ofCentipawns(0);
            List<String> lastPv = new ArrayList<>();
            int lastDepth = 0;
            String bestMoveUci = null;

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("info") && line.contains("score")) {
                    ParsedInfo info = parseInfoLine(line, whiteToMove);
                    if (info != null) {
                        lastEval = info.evaluation;
                        lastDepth = info.depth;
                        if (!info.pv.isEmpty()) {
                            lastPv = info.pv;
                        }
                    }
                } else if (line.startsWith("bestmove")) {
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length >= 2) {
                        bestMoveUci = parts[1];
                    }
                    break;
                }
            }

            watchdog.interrupt();

            if (bestMoveUci == null || "(none)".equals(bestMoveUci)) {
                // No legal moves: checkmate or stalemate reached in this position.
                bestMoveUci = null;
            }

            return new EngineAnalysisResult(lastEval, bestMoveUci, lastPv, lastDepth);
        } catch (IOException e) {
            throw new EngineException("Lost communication with the Stockfish process.", e);
        }
    }

    private ParsedInfo parseInfoLine(String line, boolean whiteToMove) {
        String[] tokens = line.trim().split("\\s+");
        Evaluation eval = null;
        int depth = 0;
        List<String> pv = new ArrayList<>();

        for (int i = 0; i < tokens.length; i++) {
            switch (tokens[i]) {
                case "depth" -> {
                    if (i + 1 < tokens.length) {
                        depth = safeInt(tokens[i + 1], depth);
                    }
                }
                case "score" -> {
                    if (i + 2 < tokens.length) {
                        String type = tokens[i + 1]; // "cp" or "mate"
                        int value = safeInt(tokens[i + 2], 0);
                        // UCI scores are from the perspective of the side to move;
                        // normalize to White's perspective for storage.
                        int normalized = whiteToMove ? value : -value;
                        if ("mate".equals(type)) {
                            // "score mate 0" means the side to move is already checkmated.
                            eval = (value == 0)
                                    ? Evaluation.ofCheckmate(!whiteToMove)
                                    : Evaluation.ofMate(normalized);
                        } else {
                            eval = Evaluation.ofCentipawns(normalized);
                        }
                    }
                }
                case "pv" -> {
                    for (int j = i + 1; j < tokens.length; j++) {
                        pv.add(tokens[j]);
                    }
                }
                default -> { /* ignore nps, nodes, hashfull, tbhits, multipv, etc. */ }
            }
        }

        if (eval == null) return null;
        return new ParsedInfo(eval, depth, pv);
    }

    private int safeInt(String s, int fallback) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private void send(String command) throws IOException {
        writer.write(command);
        writer.write("\n");
        writer.flush();
    }

    private void waitFor(String token, long timeoutMs) throws IOException, EngineException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.contains(token)) {
                return;
            }
            if (System.currentTimeMillis() > deadline) {
                throw new EngineException("Timed out waiting for Stockfish to respond with '" + token + "'.");
            }
        }
        throw new EngineException("Stockfish process ended unexpectedly during startup.");
    }

    /** Sends "stop" if a search is in flight; safe to call even if nothing is running. */
    public void stopSearch() {
        try {
            if (isRunning()) {
                send("stop");
            }
        } catch (IOException ignored) {
        }
    }

    /** Cleanly shuts down the engine and releases the process. */
    public void quit() {
        try {
            if (isRunning()) {
                send("quit");
            }
        } catch (IOException ignored) {
        } finally {
            running = false;
            if (process != null) {
                process.destroy();
                try {
                    if (!process.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)) {
                        process.destroyForcibly();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    process.destroyForcibly();
                }
            }
        }
    }

    @Override
    public void close() {
        quit();
    }

    private record ParsedInfo(Evaluation evaluation, int depth, List<String> pv) {
    }
}

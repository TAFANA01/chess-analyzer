package com.chessanalyzer.model;

/**
 * User-configurable settings for a single analysis run.
 */
public class AnalysisSettings {

    public enum Mode {
        QUICK(12),
        NORMAL(18),
        DEEP(24);

        private final int depth;

        Mode(int depth) {
            this.depth = depth;
        }

        public int depth() {
            return depth;
        }
    }

    private Mode mode = Mode.NORMAL;
    private String stockfishPath;
    /** Max milliseconds to let the engine think per position, as a safety cap alongside depth. */
    private int moveTimeLimitMs = 15_000;
    /** Number of threads Stockfish is allowed to use. */
    private int engineThreads = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
    /** Hash table size in MB given to the engine. */
    private int engineHashMb = 256;

    public AnalysisSettings() {
    }

    public AnalysisSettings(Mode mode, String stockfishPath) {
        this.mode = mode;
        this.stockfishPath = stockfishPath;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public int getDepth() {
        return mode.depth();
    }

    public String getStockfishPath() {
        return stockfishPath;
    }

    public void setStockfishPath(String stockfishPath) {
        this.stockfishPath = stockfishPath;
    }

    public int getMoveTimeLimitMs() {
        return moveTimeLimitMs;
    }

    public void setMoveTimeLimitMs(int moveTimeLimitMs) {
        this.moveTimeLimitMs = moveTimeLimitMs;
    }

    public int getEngineThreads() {
        return engineThreads;
    }

    public void setEngineThreads(int engineThreads) {
        this.engineThreads = engineThreads;
    }

    public int getEngineHashMb() {
        return engineHashMb;
    }

    public void setEngineHashMb(int engineHashMb) {
        this.engineHashMb = engineHashMb;
    }
}

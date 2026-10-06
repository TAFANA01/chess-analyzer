package com.chessanalyzer.service;

/** Reports progress of a running game analysis; implemented by the UI layer. */
public interface AnalysisProgressListener {

    /**
     * @param currentMove  1-based index of the ply just completed
     * @param totalMoves   total plies in the game
     * @param moveLabel    e.g. "32...Qe7"
     * @param engineDepth  the configured search depth
     */
    void onProgress(int currentMove, int totalMoves, String moveLabel, int engineDepth);
}

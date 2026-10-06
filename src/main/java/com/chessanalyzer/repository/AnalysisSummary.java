package com.chessanalyzer.repository;

import com.chessanalyzer.model.GameAnalysis;
import com.chessanalyzer.model.MoveClassification;

import java.io.File;
import java.time.LocalDateTime;

/** Lightweight, immutable view of a saved analysis for history lists and progress statistics. */
public record AnalysisSummary(String id, File file, LocalDateTime analyzedAt,
                              String white, String black, String result, String gameDate,
                              String opening, String eco, int plies,
                              double whiteAccuracy, double blackAccuracy,
                              int whiteBlunders, int blackBlunders,
                              int whiteMistakes, int blackMistakes) {

    public static AnalysisSummary of(GameAnalysis a, File file) {
        var g = a.getGame();
        var st = a.getMoveStatistics();
        return new AnalysisSummary(a.getId(), file, a.getAnalyzedAt(),
                g.getWhite(), g.getBlack(), g.getResult(), g.getDate(),
                g.getOpening(), g.getEco(), a.getMoves().size(),
                a.getWhiteAccuracy(), a.getBlackAccuracy(),
                st.getCount(true, MoveClassification.BLUNDER), st.getCount(false, MoveClassification.BLUNDER),
                st.getCount(true, MoveClassification.MISTAKE), st.getCount(false, MoveClassification.MISTAKE));
    }
}

package com.chessanalyzer.support;

import com.chessanalyzer.model.*;
import com.chessanalyzer.service.AnalysisRecalculator;

/** Builders for small hand-made analyses so persistence/statistics tests need no engine. */
public final class TestData {

    private TestData() {
    }

    public static final String SIMPLE_PGN = """
            [Event "Test"]
            [Site "Nowhere"]
            [Date "2026.01.02"]
            [Round "1"]
            [White "Alice"]
            [Black "Bob"]
            [Result "1-0"]
            [WhiteElo "1500"]
            [BlackElo "1480"]
            [TimeControl "300+2"]
            [ECO "C20"]
            [Opening "King's Pawn Game"]

            1. e4 e5 2. Qh5 Nc6 3. Bc4 Nf6 4. Qxf7# 1-0
            """;

    public static MoveAnalysis move(int ply, String san, String uci, boolean white,
                                    Evaluation before, Evaluation after, String bestUci) {
        MoveAnalysis m = new MoveAnalysis();
        m.setMoveNumber(ply / 2 + 1);
        m.setWhiteToMove(white);
        m.setSan(san);
        m.setUci(uci);
        m.setFenBefore("8/8/8/8/8/8/8/K6k w - - 0 1");
        m.setFenAfter("8/8/8/8/8/8/8/K6k b - - 0 1");
        m.setEvalBefore(before);
        m.setEvalAfter(after);
        m.setBestMoveUci(bestUci);
        m.setBestMoveSan(san);
        m.setEngineDepth(12);
        return m;
    }

    /** A 4-ply analysis with consistent derived data (computed by the production recalculation code). */
    public static GameAnalysis sampleAnalysis(String white, String black) {
        ChessGame game = new ChessGame();
        game.setWhite(white);
        game.setBlack(black);
        game.setResult("1-0");
        game.setDate("2026.01.02");
        game.setOpening("Test Opening");
        game.setEco("C20");
        for (String s : new String[]{"e4", "e5", "Nf3", "Nc6"}) {
            game.getMovesSan().add(s);
        }
        GameAnalysis a = new GameAnalysis();
        a.setGame(game);
        a.setSettings(new AnalysisSettings(AnalysisSettings.Mode.QUICK, "/usr/games/stockfish"));
        a.getMoves().add(move(0, "e4", "e2e4", true, Evaluation.ofCentipawns(20), Evaluation.ofCentipawns(20), "e2e4"));
        a.getMoves().add(move(1, "e5", "e7e5", false, Evaluation.ofCentipawns(20), Evaluation.ofCentipawns(25), "e7e5"));
        a.getMoves().add(move(2, "Nf3", "g1f3", true, Evaluation.ofCentipawns(25), Evaluation.ofCentipawns(-150), "d2d4"));
        a.getMoves().add(move(3, "Nc6", "b8c6", false, Evaluation.ofCentipawns(-150), Evaluation.ofCentipawns(-140), "b8c6"));
        new AnalysisRecalculator().recalculate(a); // loss + classification + derived stats, as in a real run
        return a;
    }
}

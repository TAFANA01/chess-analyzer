package com.chessanalyzer.support;

import com.chessanalyzer.model.*;
import com.chessanalyzer.service.PgnService;
import com.chessanalyzer.util.FenUtil;
import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.move.Move;

/**
 * Builds {@link GameAnalysis} objects from PGN by replaying with chesslib so navigation/board tests can
 * run on real positions (real FENs, real UCI) without an engine. Evaluations here are neutral placeholders
 * and exist only so the model is complete; tests that care about engine data use the Stockfish integration
 * tests or set the fields they assert on explicitly.
 */
public final class TestGames {

    public static final String RUY_LOPEZ_CASTLING = "1. e4 e5 2. Nf3 Nc6 3. Bb5 a6 4. Ba4 Nf6 5. O-O Be7 6. Re1 b5 7. Bb3 d6 8. c3 O-O *";
    public static final String EN_PASSANT = "1. e4 a6 2. e5 d5 3. exd6 *";
    public static final String PROMOTION = "1. a4 b5 2. axb5 a6 3. bxa6 Nc6 4. a7 Nb8 5. axb8=Q *";
    public static final String SCHOLARS_MATE = "[Result \"1-0\"]\n\n1. e4 e5 2. Qh5 Nc6 3. Bc4 Nf6 4. Qxf7# 1-0";
    public static final String FOOLS_MATE = "[Result \"0-1\"]\n\n1. f3 e5 2. g4 Qh4# 0-1";
    public static final String DRAW = "[Result \"1/2-1/2\"]\n\n1. Nf3 Nf6 2. Ng1 Ng8 3. Nf3 Nf6 4. Ng1 Ng8 1/2-1/2";
    public static final String ONE_MOVE = "[Result \"*\"]\n\n1. e4 *";

    private TestGames() {
    }

    public static GameAnalysis analysis(String pgn) {
        try {
            ChessGame game = new PgnService().parse(pgn);
            GameAnalysis a = new GameAnalysis();
            a.setGame(game);
            Board board = new Board();
            if (game.getStartFen() != null) board.loadFromFen(game.getStartFen());
            for (String san : game.getMovesSan()) {
                String before = board.getFen();
                boolean white = board.getSideToMove() == com.github.bhlangonijr.chesslib.Side.WHITE;
                board.doMove(san);
                Move played = board.getBackup().getLast().getMove();
                MoveAnalysis m = new MoveAnalysis();
                m.setMoveNumber(FenUtil.fullmoveNumber(before));
                m.setWhiteToMove(white);
                m.setSan(san);
                m.setUci(played.toString());
                m.setFenBefore(before);
                m.setFenAfter(board.getFen());
                m.setEvalBefore(Evaluation.ofCentipawns(10));
                m.setEvalAfter(Evaluation.ofCentipawns(10));
                m.setClassification(MoveClassification.GOOD);
                m.setBestMoveUci(played.toString());
                m.setBestMoveSan(san);
                a.getMoves().add(m);
            }
            return a;
        } catch (Exception e) {
            throw new IllegalStateException("Bad test PGN: " + pgn, e);
        }
    }

    /** Marks the move at 0-based index {@code moveIndex} as a classified move with a given engine alternative. */
    public static MoveAnalysis mark(GameAnalysis a, int moveIndex, MoveClassification c,
                                    String bestUci, String bestSan, double loss, Evaluation before, Evaluation after) {
        MoveAnalysis m = a.getMoves().get(moveIndex);
        m.setClassification(c);
        m.setBestMoveUci(bestUci);
        m.setBestMoveSan(bestSan);
        m.setEvalLossCentipawns(loss);
        m.setEvalBefore(before);
        m.setEvalAfter(after);
        return m;
    }
}

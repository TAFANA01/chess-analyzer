package com.chessanalyzer.integration;

import com.chessanalyzer.board.*;
import com.chessanalyzer.model.*;
import com.chessanalyzer.service.AnalysisService;
import com.chessanalyzer.service.PgnService;
import com.chessanalyzer.support.TestGames;
import com.chessanalyzer.util.FileUtil;
import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.move.Move;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Runs real Stockfish analyses of the special-move scenarios and checks that everything the board would
 * draw from them (positions, highlights, arrows, evaluations) is consistent and legal. Skipped without Stockfish.
 */
class BoardWithRealEngineIT {

    private static String stockfish;

    @BeforeAll
    static void locate() {
        String configured = System.getProperty("stockfish.path");
        stockfish = FileUtil.isValidExecutable(configured) ? configured : FileUtil.tryAutoDetectStockfish();
        assumeTrue(stockfish != null, "Stockfish not available - skipped");
    }

    private static GameAnalysis analyse(String pgn) throws Exception {
        AnalysisSettings s = new AnalysisSettings(AnalysisSettings.Mode.QUICK, stockfish);
        s.setEngineThreads(1);
        s.setEngineHashMb(32);
        return new AnalysisService().analyzeGame(new PgnService().parse(pgn), s, null, null);
    }

    private static boolean legal(String fen, String uci) {
        Board b = new Board();
        b.loadFromFen(fen);
        for (Move m : b.legalMoves()) {
            if (m.toString().equalsIgnoreCase(uci)) return true;
        }
        return false;
    }

    @Test
    void everyScenarioDisplaysExactPositionsAndLegalEngineArrows() throws Exception {
        String[] pgns = {TestGames.RUY_LOPEZ_CASTLING, TestGames.EN_PASSANT, TestGames.PROMOTION, TestGames.SCHOLARS_MATE,
                TestGames.FOOLS_MATE, TestGames.DRAW, TestGames.ONE_MOVE};
        for (String pgn : pgns) {
            GameAnalysis a = analyse(pgn);
            GameNavigator nav = new GameNavigator(a);
            Board replay = new Board();
            BoardOptions all = new BoardOptions(true, false);
            for (int ply = 0; ply <= a.getMoves().size(); ply++) {
                nav.goTo(ply);
                if (ply > 0) assertTrue(replay.doMove(a.getMoves().get(ply - 1).getSan()));
                PositionView v = nav.position();
                assertEquals(replay.getFen(), v.fen(), "ply " + ply + " of " + pgn);
                assertNotNull(v.evaluation(), "every position has an engine evaluation (ply " + ply + ")");
                if (!v.isFinal()) {
                    assertNotNull(v.bestMoveUci());
                    assertTrue(legal(v.fen(), v.bestMoveUci()), "engine suggestion legal at ply " + ply + ": " + v.bestMoveUci());
                    assertTrue(v.depth() > 0, "depth recorded at ply " + ply);
                }
                BoardScene scene = BoardScenePlanner.plan(v, all);
                for (BoardArrow arrow : scene.arrows()) {
                    String uci = Squares.name(arrow.from()) + Squares.name(arrow.to());
                    assertTrue(legal(arrow.kind() == BoardArrow.Kind.BEST_ALTERNATIVE ? v.playedMove().getFenBefore() : v.fen(),
                            uci) || uci.length() == 4, "arrow " + arrow.kind() + " " + uci);
                }
                if (v.playedMove() != null && ClassificationStyle.of(v.playedMove().getClassification()).isProblem()) {
                    MoveAnalysis m = v.playedMove();
                    assertTrue(scene.arrows().stream().anyMatch(x -> x.kind() == BoardArrow.Kind.BEST_ALTERNATIVE),
                            "problem move " + m.moveLabel() + " must show the engine alternative");
                    assertTrue(legal(m.getFenBefore(), m.getBestMoveUci()), "stored best move is legal before the move");
                }
            }
            assertTrue(nav.atEnd());
        }
    }

    @Test
    void matingGamesEndWithAMateEvaluationAndCheckmateFlag() throws Exception {
        GameNavigator whiteMates = new GameNavigator(analyse(TestGames.SCHOLARS_MATE));
        whiteMates.last();
        assertTrue(whiteMates.position().checkmate());
        assertTrue(whiteMates.position().evaluation().isMate());
        assertEquals(1, whiteMates.position().evaluation().mateSign());
        assertEquals("1-0", EvaluationFormatter.compact(whiteMates.position().evaluation()));

        GameNavigator blackMates = new GameNavigator(analyse(TestGames.FOOLS_MATE));
        blackMates.last();
        assertTrue(blackMates.position().checkmate());
        assertEquals(-1, blackMates.position().evaluation().mateSign());
        assertEquals(0.0, EvaluationFormatter.whiteShare(blackMates.position().evaluation()));
    }

    @Test
    void whiteAndBlackBlundersAreBothMarked() throws Exception {
        // 1.f3 e5 2.g4 allows Qh4#: White's 2.g4 is the blunder; Black's replies are the engine's choice.
        GameAnalysis a = analyse(TestGames.FOOLS_MATE);
        MoveAnalysis g4 = a.getMoves().get(2);
        assertEquals(MoveClassification.BLUNDER, g4.getClassification());
        assertTrue(g4.isWhiteToMove());
        // A game where Black errs: 1.e4 f6 2.d4 g5?? 3.Qh5#
        GameAnalysis b = analyse("[Result \"1-0\"]\n\n1. e4 f6 2. d4 g5 3. Qh5# 1-0");
        MoveAnalysis g5 = b.getMoves().get(3);
        assertEquals("g5", g5.getSan());
        assertFalse(g5.isWhiteToMove());
        assertEquals(MoveClassification.BLUNDER, g5.getClassification());
        GameNavigator nav = new GameNavigator(b);
        nav.goTo(4);
        BoardScene scene = BoardScenePlanner.plan(nav.position(), BoardOptions.DEFAULT);
        assertEquals(1, scene.arrows().size());
        assertEquals(b.getMoves().get(3).getBestMoveUci(),
                Squares.name(scene.arrows().get(0).from()) + Squares.name(scene.arrows().get(0).to()));
    }
}

package com.chessanalyzer.board;

import com.chessanalyzer.model.Evaluation;
import com.chessanalyzer.model.GameAnalysis;
import com.chessanalyzer.model.MoveClassification;
import com.chessanalyzer.support.TestGames;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BoardScenePlannerTest {

    private static GameAnalysis gameWithBlunder() {
        GameAnalysis a = TestGames.analysis(TestGames.RUY_LOPEZ_CASTLING);
        // 3...a6 (index 5, wrong on purpose): engine wanted Nf6; evals White-perspective.
        TestGames.mark(a, 5, MoveClassification.BLUNDER, "g8f6", "Nf6", 320,
                Evaluation.ofCentipawns(30), Evaluation.ofCentipawns(350));
        return a;
    }

    private static BoardScene sceneAt(GameAnalysis a, int ply, BoardOptions o) {
        GameNavigator nav = new GameNavigator(a);
        nav.goTo(ply);
        return BoardScenePlanner.plan(nav.position(), o);
    }

    @Test
    void initialPositionHasNoHighlightAndNoArrows() {
        BoardScene s = sceneAt(gameWithBlunder(), 0, BoardOptions.DEFAULT);
        assertEquals(Squares.NONE, s.lastFrom());
        assertEquals(Squares.NONE, s.lastTo());
        assertTrue(s.arrows().isEmpty());
    }

    @Test
    void normalMoveShowsOnlyTheLastMoveHighlight() {
        BoardScene s = sceneAt(gameWithBlunder(), 1, BoardOptions.DEFAULT);
        assertEquals(Squares.parse("e2"), s.lastFrom());
        assertEquals(Squares.parse("e4"), s.lastTo());
        assertTrue(s.arrows().isEmpty(), "no best-move clutter after a normal move");
    }

    @Test
    void blunderShowsPlayedHighlightAndEngineArrow() {
        BoardScene s = sceneAt(gameWithBlunder(), 6, BoardOptions.DEFAULT);
        assertEquals(Squares.parse("a7"), s.lastFrom());
        assertEquals(Squares.parse("a6"), s.lastTo());
        assertEquals(MoveClassification.BLUNDER, s.lastClassification());
        assertEquals(1, s.arrows().size());
        BoardArrow arrow = s.arrows().get(0);
        assertEquals(BoardArrow.Kind.BEST_ALTERNATIVE, arrow.kind());
        assertEquals(Squares.parse("g8"), arrow.from());
        assertEquals(Squares.parse("f6"), arrow.to());
    }

    @Test
    void arrowComesFromTheStoredAnalysisNotFromAnywhereElse() {
        GameAnalysis a = gameWithBlunder();
        a.getMoves().get(5).setBestMoveUci("d7d5");
        BoardArrow arrow = sceneAt(a, 6, BoardOptions.DEFAULT).arrows().get(0);
        assertEquals(Squares.parse("d7"), arrow.from());
        assertEquals(Squares.parse("d5"), arrow.to());
    }

    @Test
    void inaccuracyAndMistakeAlsoGetTheArrowButGoodMovesDoNot() {
        for (MoveClassification c : MoveClassification.values()) {
            GameAnalysis a = TestGames.analysis(TestGames.RUY_LOPEZ_CASTLING);
            TestGames.mark(a, 5, c, "g8f6", "Nf6", 50, Evaluation.ofCentipawns(0), Evaluation.ofCentipawns(0));
            boolean expectArrow = c == MoveClassification.INACCURACY || c == MoveClassification.MISTAKE || c == MoveClassification.BLUNDER;
            assertEquals(expectArrow, !sceneAt(a, 6, BoardOptions.DEFAULT).arrows().isEmpty(), c.name());
        }
    }

    @Test
    void noArrowWhenThePlayedMoveWasAlreadyTheEngineMove() {
        GameAnalysis a = TestGames.analysis(TestGames.RUY_LOPEZ_CASTLING);
        TestGames.mark(a, 5, MoveClassification.INACCURACY, "a7a6", "a6", 40, Evaluation.ofCentipawns(0), Evaluation.ofCentipawns(-40));
        assertTrue(sceneAt(a, 6, BoardOptions.DEFAULT).arrows().isEmpty());
    }

    @Test
    void engineSuggestionIsShownOnlyOnRequestAndPointsAtTheNextBestMove() {
        GameAnalysis a = gameWithBlunder();
        BoardOptions on = new BoardOptions(true, false);
        BoardScene normal = sceneAt(a, 2, BoardOptions.DEFAULT);
        BoardScene asked = sceneAt(a, 2, on);
        assertTrue(normal.arrows().isEmpty());
        assertEquals(1, asked.arrows().size());
        assertEquals(BoardArrow.Kind.SUGGESTION, asked.arrows().get(0).kind());
        // Position after ply 2 -> engine's best is the stored best move of move index 2 (3. Bb5).
        assertEquals(a.getMoves().get(2).getBestMoveUci(),
                Squares.name(asked.arrows().get(0).from()) + Squares.name(asked.arrows().get(0).to()));
    }

    @Test
    void noSuggestionAtTheFinalPosition() {
        GameAnalysis a = gameWithBlunder();
        assertTrue(sceneAt(a, a.getMoves().size(), new BoardOptions(true, false)).arrows().isEmpty());
    }

    @Test
    void compareModeShowsThePositionBeforeWithPlayedAndBestArrows() {
        GameAnalysis a = gameWithBlunder();
        BoardScene s = sceneAt(a, 6, new BoardOptions(false, true));
        assertEquals(a.getMoves().get(5).getFenBefore(), s.fen());
        assertEquals(2, s.arrows().size());
        assertTrue(s.arrows().stream().anyMatch(x -> x.kind() == BoardArrow.Kind.PLAYED
                && x.from() == Squares.parse("a7") && x.to() == Squares.parse("a6")));
        assertTrue(s.arrows().stream().anyMatch(x -> x.kind() == BoardArrow.Kind.BEST_ALTERNATIVE
                && x.from() == Squares.parse("g8") && x.to() == Squares.parse("f6")));
        // the arrow origins hold the right pieces in the before-position
        FenPosition p = FenPosition.parse(s.fen());
        assertEquals('p', p.pieceAt(Squares.parse("a7")));
        assertEquals('n', p.pieceAt(Squares.parse("g8")));
    }

    @Test
    void compareModeIsIgnoredForNormalMoves() {
        GameAnalysis a = gameWithBlunder();
        BoardScene s = sceneAt(a, 3, new BoardOptions(false, true));
        assertEquals(a.getMoves().get(2).getFenAfter(), s.fen());
        assertTrue(s.arrows().isEmpty());
    }

    @Test
    void kingInCheckIsMarked() {
        GameAnalysis a = TestGames.analysis(TestGames.SCHOLARS_MATE);
        BoardScene s = sceneAt(a, 7, BoardOptions.DEFAULT);
        assertEquals(Squares.parse("e8"), s.checkSquare());
        assertEquals(Squares.NONE, sceneAt(a, 6, BoardOptions.DEFAULT).checkSquare());
    }

    @Test
    void arrowRejectsDegenerateSquares() {
        assertThrows(IllegalArgumentException.class, () -> new BoardArrow(3, 3, BoardArrow.Kind.SUGGESTION));
        assertNull(BoardArrow.fromUci("zz", BoardArrow.Kind.SUGGESTION));
        assertNull(BoardArrow.fromUci(null, BoardArrow.Kind.SUGGESTION));
        assertEquals(Squares.parse("e7"), BoardArrow.fromUci("e7e8q", BoardArrow.Kind.SUGGESTION).from());
    }
}

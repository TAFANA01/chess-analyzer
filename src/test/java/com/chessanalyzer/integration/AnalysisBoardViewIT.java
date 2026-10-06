package com.chessanalyzer.integration;

import com.chessanalyzer.board.*;
import com.chessanalyzer.model.*;
import com.chessanalyzer.support.FxTestSupport;
import com.chessanalyzer.support.TestGames;
import com.chessanalyzer.ui.AnalysisBoardView;
import com.chessanalyzer.ui.ChessBoardView;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.chessanalyzer.support.FxTestSupport.onFx;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Drives the real analysis screen with real events. Needs a display (run under xvfb-run when headless);
 * otherwise the tests are skipped.
 */
class AnalysisBoardViewIT {

    @BeforeAll
    static void toolkit() {
        assumeTrue(FxTestSupport.start(), "No display / JavaFX toolkit - UI tests skipped");
    }

    private static GameAnalysis gameWithBlunder(String pgn) {
        GameAnalysis a = TestGames.analysis(pgn);
        if (a.getMoves().size() > 6) {
            TestGames.mark(a, 5, MoveClassification.BLUNDER, "g8f6", "Nf6", 320,
                    Evaluation.ofCentipawns(30), Evaluation.ofCentipawns(350));
        }
        return a;
    }

    private static AnalysisBoardView open(GameAnalysis a) throws Exception {
        return onFx(() -> {
            AnalysisBoardView v = new AnalysisBoardView(a, null);
            Scene scene = new Scene(v, 1240, 820);
            scene.getStylesheets().add(AnalysisBoardViewIT.class.getResource("/css/style.css").toExternalForm());
            scene.getStylesheets().add(AnalysisBoardViewIT.class.getResource("/css/board.css").toExternalForm());
            pump(v);
            return v;
        });
    }

    private static void pump(Node v) {
        v.applyCss();
        ((javafx.scene.layout.Region) v).layout();
    }

    private static BoardScene scene(AnalysisBoardView v) {
        return ((ChessBoardView) v.lookup(".chess-board")).boardSceneProperty().get();
    }

    private static Button button(AnalysisBoardView v, String tooltipStart) {
        for (Node n : v.lookupAll(".nav-button")) {
            Button b = (Button) n;
            if (b.getTooltip() != null && b.getTooltip().getText().startsWith(tooltipStart)) return b;
        }
        throw new AssertionError("no button " + tooltipStart);
    }

    private static ToggleButton toggle(AnalysisBoardView v, String text) {
        for (Node n : v.lookupAll(".toggle-button")) {
            if (((ToggleButton) n).getText().equals(text)) return (ToggleButton) n;
        }
        throw new AssertionError("no toggle " + text);
    }

    private static void key(AnalysisBoardView v, KeyCode c) {
        Event.fireEvent(v, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", c, false, false, false, false));
    }

    @Test
    void boardAlwaysShowsThePositionOfTheSelectedMove() throws Exception {
        GameAnalysis a = gameWithBlunder(TestGames.RUY_LOPEZ_CASTLING);
        AnalysisBoardView v = open(a);
        onFx(() -> {
            assertEquals(new com.github.bhlangonijr.chesslib.Board().getFen(), scene(v).fen());
            for (int i = 0; i < a.getMoves().size(); i++) {
                button(v, "Next").fire();
                pump(v);
                assertEquals(a.getMoves().get(i).getFenAfter(), scene(v).fen(), "after Next #" + (i + 1));
                assertEquals(Squares.uciFrom(a.getMoves().get(i).getUci()), scene(v).lastFrom());
                assertEquals(Squares.uciTo(a.getMoves().get(i).getUci()), scene(v).lastTo());
                assertEquals("Move " + (i + 1) + " / " + a.getMoves().size(), ((Label) v.lookup(".move-counter")).getText());
            }
            for (int i = a.getMoves().size() - 1; i >= 0; i--) {
                button(v, "Previous").fire();
                pump(v);
                String expected = i == 0 ? new com.github.bhlangonijr.chesslib.Board().getFen() : a.getMoves().get(i - 1).getFenAfter();
                assertEquals(expected, scene(v).fen(), "after Previous to ply " + i);
            }
        });
    }

    @Test
    void blunderShowsArrowHeadlineAndBothMoves() throws Exception {
        GameAnalysis a = gameWithBlunder(TestGames.RUY_LOPEZ_CASTLING);
        AnalysisBoardView v = open(a);
        onFx(() -> {
            v.navigator().goTo(6);
            pump(v);
            BoardScene s = scene(v);
            assertEquals(1, s.arrows().size());
            assertEquals(BoardArrow.Kind.BEST_ALTERNATIVE, s.arrows().get(0).kind());
            assertTrue(((Label) v.lookup(".class-headline")).getText().contains("BLUNDER"));
            assertEquals(2, v.lookupAll(".move-box").size(), "YOUR MOVE and ENGINE boxes");
            assertFalse(toggle(v, "Before move").isDisabled());

            v.navigator().goTo(3);
            pump(v);
            assertTrue(scene(v).arrows().isEmpty(), "no clutter after a normal move");
            assertEquals(0, v.lookupAll(".move-box").size());
            assertTrue(toggle(v, "Before move").isDisabled());
        });
    }

    @Test
    void showBestMoveToggleAndCompareModeWork() throws Exception {
        GameAnalysis a = gameWithBlunder(TestGames.RUY_LOPEZ_CASTLING);
        AnalysisBoardView v = open(a);
        onFx(() -> {
            v.navigator().goTo(2);
            pump(v);
            assertTrue(scene(v).arrows().isEmpty());
            toggle(v, "Show best move").fire();
            pump(v);
            assertEquals(BoardArrow.Kind.SUGGESTION, scene(v).arrows().get(0).kind());
            toggle(v, "Show best move").fire();

            v.navigator().goTo(6);
            toggle(v, "Before move").fire();
            pump(v);
            assertEquals(a.getMoves().get(5).getFenBefore(), scene(v).fen());
            assertEquals(2, scene(v).arrows().size());
            toggle(v, "Before move").fire();
            pump(v);
            assertEquals(a.getMoves().get(5).getFenAfter(), scene(v).fen());
        });
    }

    @Test
    void moveListClickJumpsAndHighlightsExactlyOneMove() throws Exception {
        GameAnalysis a = gameWithBlunder(TestGames.RUY_LOPEZ_CASTLING);
        AnalysisBoardView v = open(a);
        onFx(() -> {
            HBox target = null;
            int expectedPly = -1;
            for (Node n : v.lookupAll(".move-cell")) {
                if (n instanceof HBox cell && cell.getParent() instanceof HBox line && line.getChildren().size() == 3
                        && line.getParent() instanceof ListCell<?> lc && lc.getItem() instanceof MoveRows.Row row) {
                    int ply = line.getChildren().indexOf(cell) == 1 ? row.whitePly() : row.blackPly();
                    if (ply == 9) {
                        target = cell;
                        expectedPly = ply;
                    }
                }
            }
            assertNotNull(target, "cell for ply 9 present");
            Event.fireEvent(target, new MouseEvent(MouseEvent.MOUSE_CLICKED, 1, 1, 1, 1, MouseButton.PRIMARY, 1,
                    false, false, false, false, true, false, false, true, false, false, null));
            pump(v);
            assertEquals(expectedPly, v.navigator().ply());
            assertEquals(a.getMoves().get(8).getFenAfter(), scene(v).fen());
            assertEquals(1, v.lookupAll(".selected-move").size());
        });
    }

    @Test
    void keyboardShortcutsNavigate() throws Exception {
        AnalysisBoardView v = open(gameWithBlunder(TestGames.RUY_LOPEZ_CASTLING));
        onFx(() -> {
            key(v, KeyCode.RIGHT);
            key(v, KeyCode.RIGHT);
            assertEquals(2, v.navigator().ply());
            key(v, KeyCode.LEFT);
            assertEquals(1, v.navigator().ply());
            key(v, KeyCode.END);
            assertTrue(v.navigator().atEnd());
            key(v, KeyCode.HOME);
            assertEquals(0, v.navigator().ply());
            key(v, KeyCode.RIGHT); // beyond-the-end / before-start presses must not throw or wrap
            key(v, KeyCode.END);
            key(v, KeyCode.RIGHT);
            assertTrue(v.navigator().atEnd());
        });
    }

    @Test
    void finalPositionShowsGameOverForEveryResult() throws Exception {
        String[][] games = {{TestGames.SCHOLARS_MATE, "White 1 - 0 Black"}, {TestGames.FOOLS_MATE, "White 0 - 1 Black"},
                {TestGames.DRAW, "White \u00BD - \u00BD Black"}};
        for (String[] g : games) {
            AnalysisBoardView v = open(TestGames.analysis(g[0]));
            onFx(() -> {
                assertNull(v.lookup(".game-over"), "no banner mid-game");
                v.navigator().last();
                pump(v);
                assertNotNull(v.lookup(".game-over"));
                boolean found = false;
                for (Node n : v.lookupAll(".game-over-result")) found |= ((Label) n).getText().equals(g[1]);
                assertTrue(found, "expected '" + g[1] + "'");
                assertEquals(v.navigator().moves().get(v.navigator().plyCount() - 1).getFenAfter(), scene(v).fen());
            });
        }
    }

    @Test
    void checkmatedKingIsMarkedAndFinalMoveHighlighted() throws Exception {
        AnalysisBoardView v = open(TestGames.analysis(TestGames.SCHOLARS_MATE));
        onFx(() -> {
            v.navigator().last();
            pump(v);
            assertEquals(Squares.parse("e8"), scene(v).checkSquare());
            assertEquals(Squares.parse("h5"), scene(v).lastFrom());
            assertEquals(Squares.parse("f7"), scene(v).lastTo());
        });
    }

    @Test
    void oneMoveGameWorks() throws Exception {
        AnalysisBoardView v = open(TestGames.analysis(TestGames.ONE_MOVE));
        onFx(() -> {
            button(v, "Next").fire();
            pump(v);
            assertTrue(v.navigator().atEnd());
            assertTrue(button(v, "Next").isDisabled());
            assertEquals("Move 1 / 1", ((Label) v.lookup(".move-counter")).getText());
        });
    }

    @Test
    void theBoardIsReadOnly() throws Exception {
        GameAnalysis a = TestGames.analysis(TestGames.RUY_LOPEZ_CASTLING);
        String before = a.getMoves().stream().map(m -> m.getSan() + m.getFenAfter()).reduce("", String::concat);
        AnalysisBoardView v = open(a);
        onFx(() -> {
            v.navigator().goTo(4);
            String fen = scene(v).fen();
            ChessBoardView board = (ChessBoardView) v.lookup(".chess-board");
            for (MouseEvent type : new MouseEvent[]{
                    new MouseEvent(MouseEvent.MOUSE_PRESSED, 100, 100, 100, 100, MouseButton.PRIMARY, 1, false, false, false, false, true, false, false, true, false, false, null),
                    new MouseEvent(MouseEvent.MOUSE_DRAGGED, 200, 200, 200, 200, MouseButton.PRIMARY, 1, false, false, false, false, true, false, false, true, false, false, null),
                    new MouseEvent(MouseEvent.MOUSE_RELEASED, 200, 200, 200, 200, MouseButton.PRIMARY, 1, false, false, false, false, false, false, false, true, false, false, null)}) {
                Event.fireEvent(board.canvas(), type);
            }
            assertEquals(4, v.navigator().ply());
            assertEquals(fen, scene(v).fen());
        });
        String after = a.getMoves().stream().map(m -> m.getSan() + m.getFenAfter()).reduce("", String::concat);
        assertEquals(before, after, "analysis unchanged");
    }

    @Test
    void flippingTheBoardKeepsThePosition() throws Exception {
        AnalysisBoardView v = open(gameWithBlunder(TestGames.RUY_LOPEZ_CASTLING));
        onFx(() -> {
            v.navigator().goTo(6);
            String fen = scene(v).fen();
            for (Node n : v.lookupAll(".button")) {
                if (n instanceof Button b && "Flip board".equals(b.getText())) b.fire();
            }
            pump(v);
            assertTrue(((ChessBoardView) v.lookup(".chess-board")).flippedProperty().get());
            assertEquals(fen, scene(v).fen());
        });
    }
}

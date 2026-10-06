package com.chessanalyzer.board;

import com.chessanalyzer.model.GameAnalysis;
import com.chessanalyzer.model.MoveAnalysis;
import com.chessanalyzer.support.TestGames;
import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.move.Move;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameNavigatorTest {

    private static final List<String> ALL_GAMES = List.of(TestGames.RUY_LOPEZ_CASTLING, TestGames.EN_PASSANT,
            TestGames.PROMOTION, TestGames.SCHOLARS_MATE, TestGames.FOOLS_MATE, TestGames.DRAW, TestGames.ONE_MOVE);

    @Test
    void startsOnTheInitialPosition() {
        GameNavigator nav = new GameNavigator(TestGames.analysis(TestGames.RUY_LOPEZ_CASTLING));
        assertEquals(0, nav.ply());
        assertTrue(nav.atStart());
        assertFalse(nav.atEnd());
        assertEquals(new Board().getFen(), nav.position().fen());
        assertNull(nav.position().playedMove());
        assertEquals("e2e4", nav.position().nextMove().getUci());
        assertTrue(nav.position().isInitial());
    }

    /** The core guarantee: at every ply of every game the displayed FEN equals an independent replay. */
    @Test
    void everyPlyShowsExactlyTheReplayedPosition() {
        for (String pgn : ALL_GAMES) {
            GameAnalysis a = TestGames.analysis(pgn);
            GameNavigator nav = new GameNavigator(a);
            Board replay = new Board();
            assertEquals(replay.getFen(), nav.position().fen());
            for (int i = 0; i < a.getMoves().size(); i++) {
                nav.next();
                assertTrue(replay.doMove(a.getMoves().get(i).getSan()));
                assertEquals(replay.getFen(), nav.position().fen(), "ply " + (i + 1) + " of " + pgn);
                assertEquals(i + 1, nav.ply());
                assertSame(a.getMoves().get(i), nav.position().playedMove());
            }
            assertTrue(nav.atEnd());
        }
    }

    @Test
    void goingBackwardRetracesTheSamePositions() {
        GameAnalysis a = TestGames.analysis(TestGames.RUY_LOPEZ_CASTLING);
        GameNavigator nav = new GameNavigator(a);
        List<String> forward = new ArrayList<>();
        forward.add(nav.position().fen());
        while (!nav.atEnd()) {
            nav.next();
            forward.add(nav.position().fen());
        }
        for (int i = forward.size() - 1; i >= 0; i--) {
            assertEquals(forward.get(i), nav.position().fen());
            nav.previous();
        }
        assertEquals(0, nav.ply());
    }

    @Test
    void randomAccessGivesTheSamePositionAsSequentialAccess() {
        GameAnalysis a = TestGames.analysis(TestGames.RUY_LOPEZ_CASTLING);
        GameNavigator seq = new GameNavigator(a);
        List<String> fens = new ArrayList<>();
        fens.add(seq.position().fen());
        for (int i = 0; i < a.getMoves().size(); i++) {
            seq.next();
            fens.add(seq.position().fen());
        }
        GameNavigator jump = new GameNavigator(a);
        int[] order = {9, 3, 15, 0, 7, 16, 1, 12};
        for (int target : order) {
            jump.goTo(target);
            assertEquals(fens.get(target), jump.position().fen(), "jump to " + target);
        }
    }

    @Test
    void navigationIsClampedAtBothEnds() {
        GameNavigator nav = new GameNavigator(TestGames.analysis(TestGames.SCHOLARS_MATE));
        nav.previous();
        assertEquals(0, nav.ply());
        nav.goTo(-5);
        assertEquals(0, nav.ply());
        nav.goTo(999);
        assertEquals(7, nav.ply());
        nav.next();
        assertEquals(7, nav.ply());
        nav.first();
        assertEquals(0, nav.ply());
        nav.last();
        assertEquals(7, nav.ply());
    }

    @Test
    void oneMoveGameHasTwoPositions() {
        GameNavigator nav = new GameNavigator(TestGames.analysis(TestGames.ONE_MOVE));
        assertEquals(1, nav.plyCount());
        assertTrue(nav.position().isInitial());
        nav.next();
        assertTrue(nav.position().isFinal());
        assertEquals("e4", nav.position().playedMove().getSan());
        assertNull(nav.position().nextMove());
        nav.next();
        assertEquals(1, nav.ply());
    }

    @Test
    void gameWithNoMovesStillShowsAStartPosition() {
        GameAnalysis empty = new GameAnalysis();
        empty.setGame(new com.chessanalyzer.model.ChessGame());
        GameNavigator nav = new GameNavigator(empty);
        assertEquals(FenPosition.START_FEN, nav.position().fen());
        assertTrue(nav.position().isInitial() && nav.position().isFinal());
        nav.next();
        assertEquals(0, nav.ply());
    }

    @Test
    void listenersSeeAConsistentPositionAndOnlyRealChanges() {
        GameNavigator nav = new GameNavigator(TestGames.analysis(TestGames.RUY_LOPEZ_CASTLING));
        List<String> problems = new ArrayList<>();
        int[] events = {0};
        nav.plyProperty().addListener((o, was, now) -> {
            events[0]++;
            if (nav.position().ply() != now.intValue()) problems.add("ply " + now + " but position " + nav.position().ply());
        });
        nav.next();
        nav.next();
        nav.goTo(2); // no change
        nav.previous();
        assertEquals(3, events[0]);
        assertTrue(problems.isEmpty(), problems.toString());
    }

    @Test
    void navigationNeverModifiesTheAnalysis() {
        GameAnalysis a = TestGames.analysis(TestGames.RUY_LOPEZ_CASTLING);
        List<String> before = a.getMoves().stream().map(m -> m.getSan() + m.getFenAfter() + m.getClassification()).toList();
        GameNavigator nav = new GameNavigator(a);
        nav.last();
        nav.first();
        nav.goTo(5);
        assertEquals(before, a.getMoves().stream().map(m -> m.getSan() + m.getFenAfter() + m.getClassification()).toList());
    }

    @Test
    void checkAndCheckmateAreDetectedFromThePosition() {
        GameNavigator nav = new GameNavigator(TestGames.analysis(TestGames.SCHOLARS_MATE));
        nav.goTo(6); // after 3...Nf6, no check
        assertFalse(nav.position().inCheck());
        nav.goTo(7); // 4. Qxf7#
        assertTrue(nav.position().inCheck());
        assertTrue(nav.position().checkmate());
        assertTrue(nav.position().isFinal());

        GameNavigator fools = new GameNavigator(TestGames.analysis(TestGames.FOOLS_MATE));
        fools.last();
        assertTrue(fools.position().checkmate());
        assertEquals("Qh4#", fools.position().playedMove().getSan());
    }

    @Test
    void plainCheckIsNotCheckmate() {
        GameNavigator nav = new GameNavigator(TestGames.analysis("1. e4 e5 2. Bc4 Nc6 3. Bxf7+ *"));
        nav.last();
        assertTrue(nav.position().inCheck());
        assertFalse(nav.position().checkmate());
    }

    @Test
    void lastMoveSquaresMatchTheEngineMoveForCaptureCastlingEnPassantAndPromotion() {
        GameAnalysis castling = TestGames.analysis(TestGames.RUY_LOPEZ_CASTLING);
        GameNavigator nav = new GameNavigator(castling);
        nav.goTo(9); // 5. O-O
        assertEquals("O-O", nav.position().playedMove().getSan());
        assertEquals(Squares.parse("e1"), Squares.uciFrom(nav.position().playedMove().getUci()));
        assertEquals(Squares.parse("g1"), Squares.uciTo(nav.position().playedMove().getUci()));
        nav.goTo(16); // 8...O-O
        assertEquals(Squares.parse("e8"), Squares.uciFrom(nav.position().playedMove().getUci()));
        assertEquals(Squares.parse("g8"), Squares.uciTo(nav.position().playedMove().getUci()));

        GameNavigator promo = new GameNavigator(TestGames.analysis(TestGames.PROMOTION));
        promo.last();
        MoveAnalysis m = promo.position().playedMove();
        assertEquals("a7b8q", m.getUci());
        assertEquals(Squares.parse("a7"), Squares.uciFrom(m.getUci()));
        assertEquals(Squares.parse("b8"), Squares.uciTo(m.getUci()));

        GameNavigator ep = new GameNavigator(TestGames.analysis(TestGames.EN_PASSANT));
        ep.last();
        assertEquals("e5d6", ep.position().playedMove().getUci());
    }

    @Test
    void everyAnalysedMoveHighlightMatchesChesslibsMove() {
        for (String pgn : ALL_GAMES) {
            GameAnalysis a = TestGames.analysis(pgn);
            Board replay = new Board();
            for (MoveAnalysis m : a.getMoves()) {
                replay.doMove(m.getSan());
                Move mv = replay.getBackup().getLast().getMove();
                assertEquals(mv.getFrom().toString().toLowerCase(), Squares.name(Squares.uciFrom(m.getUci())));
                assertEquals(mv.getTo().toString().toLowerCase(), Squares.name(Squares.uciTo(m.getUci())));
            }
        }
    }

    @Test
    void customStartPositionAndBlackToMoveNumbering() {
        GameAnalysis a = TestGames.analysis("[SetUp \"1\"]\n[FEN \"4k3/8/8/8/8/8/4P3/4K3 b - - 0 20\"]\n\n20... Kd7 21. e4 *");
        GameNavigator nav = new GameNavigator(a);
        assertTrue(nav.position().fen().startsWith("4k3/8/8/8/8/8/4P3/4K3 b"));
        assertEquals("20...Kd7", a.getMoves().get(0).moveLabel());
        assertEquals("21.e4", a.getMoves().get(1).moveLabel());
        nav.last();
        assertTrue(nav.position().isFinal());
    }
}

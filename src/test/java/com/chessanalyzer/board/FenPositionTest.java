package com.chessanalyzer.board;

import com.chessanalyzer.support.TestGames;
import com.chessanalyzer.model.GameAnalysis;
import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Square;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FenPositionTest {

    @Test
    void startPositionHasThirtyTwoPiecesInTheRightPlaces() {
        FenPosition p = FenPosition.parse(FenPosition.START_FEN);
        int count = 0;
        for (int i = 0; i < 64; i++) if (!p.isEmpty(i)) count++;
        assertEquals(32, count);
        assertEquals('K', p.pieceAt(Squares.parse("e1")));
        assertEquals('k', p.pieceAt(Squares.parse("e8")));
        assertEquals('Q', p.pieceAt(Squares.parse("d1")));
        assertEquals('p', p.pieceAt(Squares.parse("h7")));
        assertEquals('R', p.pieceAt(Squares.parse("a1")));
        assertTrue(p.isEmpty(Squares.parse("e4")));
        assertTrue(p.whiteToMove());
        assertEquals(Squares.parse("e1"), p.kingSquare(true));
        assertEquals(Squares.parse("e8"), p.kingSquare(false));
    }

    /** Draw-model parity: for every ply of every special game, FenPosition agrees with chesslib square by square. */
    @Test
    void agreesWithChesslibOnEverySquareOfEveryPosition() {
        for (String pgn : new String[]{TestGames.RUY_LOPEZ_CASTLING, TestGames.EN_PASSANT, TestGames.PROMOTION,
                TestGames.SCHOLARS_MATE, TestGames.DRAW}) {
            GameAnalysis a = TestGames.analysis(pgn);
            GameNavigator nav = new GameNavigator(a);
            do {
                Board board = new Board();
                board.loadFromFen(nav.position().fen());
                FenPosition p = FenPosition.parse(nav.position().fen());
                for (Square sq : Square.values()) {
                    if (sq == Square.NONE) continue;
                    int idx = Squares.parse(sq.toString().toLowerCase());
                    Piece expected = board.getPiece(sq);
                    char got = p.pieceAt(idx);
                    if (expected == Piece.NONE) {
                        assertEquals(' ', got, sq + " in " + nav.position().fen());
                    } else {
                        char want = expected.getFenSymbol().charAt(0);
                        assertEquals(want, got, sq + " in " + nav.position().fen());
                    }
                }
                nav.next();
            } while (!nav.atEnd());
        }
    }

    @Test
    void enPassantRemovesTheCapturedPawnAndPromotionPlacesAQueen() {
        GameNavigator ep = new GameNavigator(TestGames.analysis(TestGames.EN_PASSANT));
        ep.last();
        FenPosition p = FenPosition.parse(ep.position().fen());
        assertEquals('P', p.pieceAt(Squares.parse("d6")));
        assertTrue(p.isEmpty(Squares.parse("d5")), "captured pawn must be gone");
        assertTrue(p.isEmpty(Squares.parse("e5")));

        GameNavigator promo = new GameNavigator(TestGames.analysis(TestGames.PROMOTION));
        promo.last();
        FenPosition q = FenPosition.parse(promo.position().fen());
        assertEquals('Q', q.pieceAt(Squares.parse("b8")));
        assertTrue(q.isEmpty(Squares.parse("a7")));
    }

    @Test
    void castlingMovesBothKingAndRook() {
        GameNavigator nav = new GameNavigator(TestGames.analysis(TestGames.RUY_LOPEZ_CASTLING));
        nav.goTo(9);
        FenPosition p = FenPosition.parse(nav.position().fen());
        assertEquals('K', p.pieceAt(Squares.parse("g1")));
        assertEquals('R', p.pieceAt(Squares.parse("f1")));
        assertTrue(p.isEmpty(Squares.parse("e1")));
        assertTrue(p.isEmpty(Squares.parse("h1")));
    }

    @Test
    void rejectsMalformedFen() {
        for (String bad : new String[]{null, "", "8/8/8", "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBN w", "9/8/8/8/8/8/8/8 w",
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNX w", "7/8/8/8/8/8/8/8 w"}) {
            assertThrows(IllegalArgumentException.class, () -> FenPosition.parse(bad), String.valueOf(bad));
        }
    }

    @Test
    void squareHelpersAreConsistent() {
        assertEquals(0, Squares.parse("a1"));
        assertEquals(63, Squares.parse("h8"));
        assertEquals(28, Squares.parse("e4"));
        assertEquals("e4", Squares.name(28));
        assertEquals(Squares.NONE, Squares.parse("i9"));
        assertEquals(Squares.NONE, Squares.uciFrom("e2"));
        for (int i = 0; i < 64; i++) assertEquals(i, Squares.parse(Squares.name(i)));
    }
}

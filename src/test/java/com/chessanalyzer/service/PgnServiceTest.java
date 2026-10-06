package com.chessanalyzer.service;

import com.chessanalyzer.model.ChessGame;
import com.chessanalyzer.support.TestData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PgnServiceTest {

    private final PgnService service = new PgnService();

    @Test
    void parsesHeadersAndMoves() throws Exception {
        ChessGame g = service.parse(TestData.SIMPLE_PGN);
        assertEquals("Alice", g.getWhite());
        assertEquals("Bob", g.getBlack());
        assertEquals("1-0", g.getResult());
        assertEquals("2026.01.02", g.getDate());
        assertEquals("Test", g.getEvent());
        assertEquals("Nowhere", g.getSite());
        assertEquals("1", g.getRound());
        assertEquals(1500, g.getWhiteElo());
        assertEquals(1480, g.getBlackElo());
        assertEquals("300+2", g.getTimeControl());
        assertEquals("C20", g.getEco());
        assertEquals("King's Pawn Game", g.getOpening());
        assertEquals(7, g.getMoveCount());
        assertEquals("Qxf7#", g.getMovesSan().get(6));
        assertNull(g.getStartFen());
    }

    @Test
    void unknownEloIsNull() throws Exception {
        ChessGame g = service.parse("[WhiteElo \"?\"]\n[BlackElo \"-\"]\n\n1. e4 e5 *");
        assertNull(g.getWhiteElo());
        assertNull(g.getBlackElo());
    }

    @Test
    void parsesMultipleGames() throws Exception {
        String pgn = TestData.SIMPLE_PGN + "\n\n[White \"Carol\"]\n[Black \"Dan\"]\n[Result \"1/2-1/2\"]\n\n1. d4 d5 1/2-1/2\n";
        PgnParseResult r = service.parseAll(pgn);
        assertEquals(2, r.games().size());
        assertFalse(r.hasErrors());
        assertEquals("Carol", r.games().get(1).getWhite());
        assertEquals(2, r.games().get(1).getMoveCount());
        assertFalse(r.games().get(0).getRawPgn().contains("Carol"), "raw PGN must be stored per game");
    }

    @Test
    void badGameDoesNotDiscardGoodOnes() throws Exception {
        String pgn = TestData.SIMPLE_PGN + "\n[White \"Eve\"]\n[Black \"Mallory\"]\n\n1. e4 e5 2. Ke3 *\n\n"
                + "[White \"Carol\"]\n[Black \"Dan\"]\n\n1. d4 d5 *\n";
        PgnParseResult r = service.parseAll(pgn);
        assertEquals(2, r.games().size());
        assertEquals(1, r.errors().size());
        assertEquals(2, r.errors().get(0).gameNumber());
        assertEquals("Eve", r.errors().get(0).white());
        assertTrue(r.errors().get(0).message().contains("Ke3"));
    }

    @Test
    void stripsCommentsNagsVariationsAndAnnotations() throws Exception {
        String pgn = "[White \"A\"]\n[Black \"B\"]\n\n"
                + "1. e4! {[%clk 0:03:00] best by test} e5?! $1 (1... c5 2. Nf3 (2. c3 d5) 2... d6) "
                + "2. Nf3 ; line comment with (parens\n"
                + "Nc6 3. Bb5 *";
        ChessGame g = service.parse(pgn);
        assertEquals(List.of("e4", "e5", "Nf3", "Nc6", "Bb5"), g.getMovesSan());
    }

    @Test
    void handlesPercentLinesBomAndWindowsLineEndings() throws Exception {
        String pgn = "\uFEFF[White \"A\"]\r\n[Black \"B\"]\r\n\r\n% escape line\r\n1. e4 e5\r\n2. Nf3 *\r\n";
        assertEquals(3, service.parse(pgn).getMoveCount());
    }

    @Test
    void acceptsZeroCastlingSpelling() throws Exception {
        ChessGame g = service.parse("1. e4 e5 2. Nf3 Nc6 3. Bc4 Bc5 4. 0-0 Nf6 *");
        assertEquals("O-O", g.getMovesSan().get(6));
    }

    @Test
    void supportsCustomStartPosition() throws Exception {
        String pgn = "[SetUp \"1\"]\n[FEN \"4k3/8/8/8/8/8/4P3/4K3 w - - 0 1\"]\n\n1. e4 Kd7 2. e5 *";
        ChessGame g = service.parse(pgn);
        assertNotNull(g.getStartFen());
        assertEquals(3, g.getMoveCount());
    }

    @Test
    void rejectsInvalidFen() {
        PgnValidationException e = assertThrows(PgnValidationException.class,
                () -> service.parse("[SetUp \"1\"]\n[FEN \"not a fen\"]\n\n1. e4 *"));
        assertTrue(e.getMessage().contains("FEN"));
    }

    @Test
    void reportsIllegalMoveWithNumberAndSide() {
        PgnValidationException e = assertThrows(PgnValidationException.class,
                () -> service.parse("1. e4 e5 2. Nf3 Nc6 3. Bb5 a6 4. Bxf7 *"));
        assertTrue(e.getMessage().contains("4. (White)"), e.getMessage());
        assertTrue(e.getMessage().contains("Bxf7"));
    }

    @Test
    void rejectsGarbageAndImpossibleMoves() {
        assertThrows(PgnValidationException.class, () -> service.parse("1. e4 e5 2. Zz9 *"));
        assertThrows(PgnValidationException.class, () -> service.parse("1. Ra3 *"));
        assertThrows(PgnValidationException.class, () -> service.parse("1. e5 *"));
    }

    @Test
    void rejectsEmptyAndBlankInput() {
        assertThrows(PgnValidationException.class, () -> service.parse(null));
        assertThrows(PgnValidationException.class, () -> service.parse("   \n  "));
    }

    @Test
    void rejectsHeaderOnlyGame() {
        PgnValidationException e = assertThrows(PgnValidationException.class,
                () -> service.parse("[White \"A\"]\n[Black \"B\"]\n[Result \"*\"]\n"));
        assertTrue(e.getMessage().toLowerCase().contains("no moves"));
    }

    @Test
    void rejectsUnbalancedVariationAndComment() {
        assertThrows(PgnValidationException.class, () -> service.parse("1. e4 (1. d4 e5 *"));
        assertThrows(PgnValidationException.class, () -> service.parse("1. e4 e5) 2. Nf3 *"));
        assertThrows(PgnValidationException.class, () -> service.parse("1. e4 {never closed e5 *"));
    }

    @Test
    void movesWithoutHeadersAreAccepted() throws Exception {
        ChessGame g = service.parse("1. e4 e5 2. Nf3 Nc6 *");
        assertEquals("Unknown", g.getWhite());
        assertEquals(4, g.getMoveCount());
    }
}

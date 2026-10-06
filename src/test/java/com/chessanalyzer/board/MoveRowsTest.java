package com.chessanalyzer.board;

import com.chessanalyzer.board.MoveRows.Row;
import com.chessanalyzer.model.GameAnalysis;
import com.chessanalyzer.support.TestGames;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MoveRowsTest {

    @Test
    void groupsWhiteAndBlackMovesIntoNumberedRows() {
        GameAnalysis a = TestGames.analysis(TestGames.RUY_LOPEZ_CASTLING); // 16 plies
        List<Row> rows = MoveRows.build(a.getMoves());
        assertEquals(1 + 8, rows.size());
        assertTrue(rows.get(0).startRow());
        Row r1 = rows.get(1);
        assertEquals(1, r1.moveNumber());
        assertEquals("e4", r1.white().getSan());
        assertEquals("e5", r1.black().getSan());
        assertEquals(1, r1.whitePly());
        assertEquals(2, r1.blackPly());
        Row r5 = rows.get(5);
        assertEquals("O-O", r5.white().getSan());
        assertEquals(9, r5.whitePly());
        assertEquals(10, r5.blackPly());
    }

    @Test
    void oddLengthGameLeavesTheLastRowWithoutBlackMove() {
        List<Row> rows = MoveRows.build(TestGames.analysis(TestGames.ONE_MOVE).getMoves());
        assertEquals(2, rows.size());
        assertNull(rows.get(1).black());
        assertEquals(-1, rows.get(1).blackPly());
    }

    @Test
    void gameStartingWithBlackPutsBlackInTheFirstRow() {
        GameAnalysis a = TestGames.analysis("[SetUp \"1\"]\n[FEN \"4k3/8/8/8/8/8/4P3/4K3 b - - 0 20\"]\n\n20... Kd7 21. e4 Ke7 *");
        List<Row> rows = MoveRows.build(a.getMoves());
        assertEquals(3, rows.size());
        assertNull(rows.get(1).white());
        assertEquals(20, rows.get(1).moveNumber());
        assertEquals(1, rows.get(1).blackPly());
        assertEquals(21, rows.get(2).moveNumber());
        assertEquals(2, rows.get(2).whitePly());
        assertEquals(3, rows.get(2).blackPly());
    }

    @Test
    void everyPlyBelongsToExactlyOneRowAndRoundTrips() {
        GameAnalysis a = TestGames.analysis(TestGames.RUY_LOPEZ_CASTLING);
        List<Row> rows = MoveRows.build(a.getMoves());
        assertEquals(0, MoveRows.rowIndexForPly(rows, 0));
        for (int ply = 1; ply <= a.getMoves().size(); ply++) {
            int idx = MoveRows.rowIndexForPly(rows, ply);
            assertTrue(idx >= 1);
            Row r = rows.get(idx);
            assertTrue(r.whitePly() == ply || r.blackPly() == ply, "ply " + ply);
        }
    }

    @Test
    void emptyGameHasOnlyTheStartRow() {
        assertEquals(1, MoveRows.build(List.of()).size());
    }
}

package com.chessanalyzer.board;

import com.chessanalyzer.model.MoveAnalysis;

import java.util.ArrayList;
import java.util.List;

/** Groups half-moves into the numbered rows of the move list ("1. e4  e5"). Pure logic, no JavaFX. */
public final class MoveRows {

    /**
     * One list row. {@code whitePly}/{@code blackPly} are the navigator positions reached by each move
     * (1-based ply), or -1 when that half-move does not exist (for example a game that starts with
     * Black's move, or one that ends on White's move). The first row is the "initial position" entry.
     */
    public record Row(boolean startRow, int moveNumber, MoveAnalysis white, int whitePly,
                      MoveAnalysis black, int blackPly) { }

    private MoveRows() {
    }

    public static List<Row> build(List<MoveAnalysis> moves) {
        List<Row> rows = new ArrayList<>();
        rows.add(new Row(true, 0, null, -1, null, -1));
        int i = 0;
        while (i < moves.size()) {
            MoveAnalysis m = moves.get(i);
            if (m.isWhiteToMove()) {
                MoveAnalysis reply = (i + 1 < moves.size() && !moves.get(i + 1).isWhiteToMove()) ? moves.get(i + 1) : null;
                rows.add(new Row(false, m.getMoveNumber(), m, i + 1, reply, reply != null ? i + 2 : -1));
                i += reply != null ? 2 : 1;
            } else {
                rows.add(new Row(false, m.getMoveNumber(), null, -1, m, i + 1));
                i += 1;
            }
        }
        return rows;
    }

    /** Index of the row that contains the given ply (0 for the initial position). */
    public static int rowIndexForPly(List<Row> rows, int ply) {
        if (ply <= 0) return 0;
        for (int r = 1; r < rows.size(); r++) {
            Row row = rows.get(r);
            if (row.whitePly() == ply || row.blackPly() == ply) return r;
        }
        return 0;
    }
}

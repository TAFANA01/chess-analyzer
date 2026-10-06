package com.chessanalyzer.board;

/** An arrow between two squares. Kind decides the colour; the renderer decides the geometry. */
public record BoardArrow(int from, int to, Kind kind) {

    public enum Kind {
        /** The engine's better move instead of the (inaccurate) move that was played. */
        BEST_ALTERNATIVE,
        /** The engine's suggestion for the side to move in the displayed position (on request). */
        SUGGESTION,
        /** The move that was actually played (shown in the "before the move" comparison view). */
        PLAYED,
        /** Reserved for second-best / MultiPV lines. */
        ALTERNATIVE_LINE
    }

    public BoardArrow {
        if (from < 0 || from > 63 || to < 0 || to > 63 || from == to) {
            throw new IllegalArgumentException("Invalid arrow " + from + " -> " + to);
        }
    }

    /** Builds an arrow from a UCI move; null if the move text is missing or malformed. */
    public static BoardArrow fromUci(String uci, Kind kind) {
        int from = Squares.uciFrom(uci);
        int to = Squares.uciTo(uci);
        return (from == Squares.NONE || to == Squares.NONE || from == to) ? null : new BoardArrow(from, to, kind);
    }
}

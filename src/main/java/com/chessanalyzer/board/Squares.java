package com.chessanalyzer.board;

/**
 * Square indexing used by the board components: {@code index = rank * 8 + file}
 * with file 0..7 = a..h and rank 0..7 = ranks 1..8, so a1 = 0 and h8 = 63.
 */
public final class Squares {

    public static final int NONE = -1;

    private Squares() {
    }

    public static int of(int file, int rank) {
        return rank * 8 + file;
    }

    public static int file(int square) {
        return square & 7;
    }

    public static int rank(int square) {
        return square >> 3;
    }

    /** "e4" -> 28; NONE if the text is not a valid square. */
    public static int parse(String name) {
        if (name == null || name.length() != 2) return NONE;
        int f = name.charAt(0) - 'a';
        int r = name.charAt(1) - '1';
        return (f < 0 || f > 7 || r < 0 || r > 7) ? NONE : of(f, r);
    }

    public static String name(int square) {
        if (square < 0 || square > 63) return "?";
        return "" + (char) ('a' + file(square)) + (char) ('1' + rank(square));
    }

    /** Origin square of a UCI move such as "e2e4" or "e7e8q"; NONE if malformed. */
    public static int uciFrom(String uci) {
        return (uci == null || uci.length() < 4) ? NONE : parse(uci.substring(0, 2));
    }

    /** Destination square of a UCI move; NONE if malformed. */
    public static int uciTo(String uci) {
        return (uci == null || uci.length() < 4) ? NONE : parse(uci.substring(2, 4));
    }
}

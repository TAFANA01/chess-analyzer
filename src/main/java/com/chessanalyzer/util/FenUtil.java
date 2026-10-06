package com.chessanalyzer.util;

/** Small helpers for reading FEN fields without needing a full board. */
public final class FenUtil {

    private FenUtil() {
    }

    /** The FEN's fullmove counter (field 6); 1 if the field is missing or malformed. */
    public static int fullmoveNumber(String fen) {
        if (fen == null) return 1;
        String[] parts = fen.trim().split("\\s+");
        if (parts.length < 6) return 1;
        try {
            return Math.max(1, Integer.parseInt(parts[5]));
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}

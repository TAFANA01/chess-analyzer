package com.chessanalyzer.board;

/**
 * Read-only piece placement parsed from a FEN string, used only to <em>draw</em> a position.
 * All chess knowledge (legality, history) stays in chesslib and in the stored
 * {@code MoveAnalysis} FENs; this class never computes moves.
 */
public final class FenPosition {

    public static final String START_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    private static final String PIECES = "KQRBNPkqrbnp";

    private final char[] squares = new char[64];
    private final boolean whiteToMove;

    private FenPosition(boolean whiteToMove) {
        java.util.Arrays.fill(squares, ' ');
        this.whiteToMove = whiteToMove;
    }

    /**
     * @throws IllegalArgumentException if the placement field is not a valid 8x8 description
     */
    public static FenPosition parse(String fen) {
        if (fen == null || fen.isBlank()) {
            throw new IllegalArgumentException("FEN is empty");
        }
        String[] fields = fen.trim().split("\\s+");
        String[] ranks = fields[0].split("/", -1);
        if (ranks.length != 8) {
            throw new IllegalArgumentException("FEN must describe 8 ranks: " + fen);
        }
        FenPosition p = new FenPosition(fields.length < 2 || !"b".equals(fields[1]));
        for (int i = 0; i < 8; i++) {
            int rank = 7 - i; // first FEN rank is rank 8
            int file = 0;
            for (char c : ranks[i].toCharArray()) {
                if (c >= '1' && c <= '8') {
                    file += c - '0';
                } else if (PIECES.indexOf(c) >= 0 && file < 8) {
                    p.squares[Squares.of(file++, rank)] = c;
                } else {
                    throw new IllegalArgumentException("Invalid FEN character '" + c + "' in " + fen);
                }
            }
            if (file != 8) {
                throw new IllegalArgumentException("FEN rank " + (rank + 1) + " does not have 8 files: " + fen);
            }
        }
        return p;
    }

    /** Piece letter on the square (uppercase = White) or ' ' when empty. */
    public char pieceAt(int square) {
        return squares[square];
    }

    public boolean isEmpty(int square) {
        return squares[square] == ' ';
    }

    public static boolean isWhite(char piece) {
        return Character.isUpperCase(piece);
    }

    public boolean whiteToMove() {
        return whiteToMove;
    }

    /** Square of the given side's king, or {@link Squares#NONE} if absent. */
    public int kingSquare(boolean white) {
        char king = white ? 'K' : 'k';
        for (int i = 0; i < 64; i++) {
            if (squares[i] == king) return i;
        }
        return Squares.NONE;
    }
}

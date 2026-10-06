package com.chessanalyzer.util;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.PieceType;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;

/** Simple material counting (pawn=100, minor=300, rook=500, queen=900) used by explanations and "brilliant" detection. */
public final class MaterialUtil {

    private MaterialUtil() {
    }

    public static int value(PieceType type) {
        if (type == null) return 0;
        return switch (type) {
            case PAWN -> 100;
            case KNIGHT, BISHOP -> 300;
            case ROOK -> 500;
            case QUEEN -> 900;
            default -> 0; // kings and NONE
        };
    }

    /** Total non-king material for one side, in centipawns. */
    public static int material(Board board, Side side) {
        int total = 0;
        for (Square sq : Square.values()) {
            if (sq == Square.NONE) continue;
            Piece p = board.getPiece(sq);
            if (p != null && p != Piece.NONE && p.getPieceSide() == side) {
                total += value(p.getPieceType());
            }
        }
        return total;
    }

    /** Material balance from {@code side}'s point of view (positive = {@code side} is ahead). */
    public static int balance(Board board, Side side) {
        return material(board, side) - material(board, side == Side.WHITE ? Side.BLACK : Side.WHITE);
    }
}

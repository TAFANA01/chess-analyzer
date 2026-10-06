package com.chessanalyzer.util;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;

/**
 * Resolves a raw UCI move string from Stockfish (e.g. "e2e4", "e7e8q") into
 * the matching {@link Move} object from the current position's legal moves,
 * so it can be rendered as SAN via {@link SanFormatter} or compared to the
 * move actually played.
 */
public final class UciMoveParser {

    private UciMoveParser() {
    }

    public static Move resolve(Board board, String uci) {
        if (uci == null || uci.length() < 4) return null;

        Square from;
        Square to;
        try {
            from = Square.valueOf(uci.substring(0, 2).toUpperCase());
            to = Square.valueOf(uci.substring(2, 4).toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }

        Piece promotion = Piece.NONE;
        if (uci.length() >= 5) {
            promotion = promotionPiece(uci.charAt(4), board.getSideToMove());
        }

        for (Move m : board.legalMoves()) {
            if (m.getFrom() != from || m.getTo() != to) continue;
            Piece movePromo = m.getPromotion();
            boolean movePromoIsNone = (movePromo == null || movePromo == Piece.NONE);
            if (promotion == Piece.NONE) {
                if (movePromoIsNone) return m;
            } else if (movePromo == promotion) {
                return m;
            }
        }
        return null;
    }

    private static Piece promotionPiece(char c, Side side) {
        boolean white = side == Side.WHITE;
        return switch (Character.toLowerCase(c)) {
            case 'q' -> white ? Piece.WHITE_QUEEN : Piece.BLACK_QUEEN;
            case 'r' -> white ? Piece.WHITE_ROOK : Piece.BLACK_ROOK;
            case 'b' -> white ? Piece.WHITE_BISHOP : Piece.BLACK_BISHOP;
            case 'n' -> white ? Piece.WHITE_KNIGHT : Piece.BLACK_KNIGHT;
            default -> Piece.NONE;
        };
    }
}

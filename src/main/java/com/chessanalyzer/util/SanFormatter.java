package com.chessanalyzer.util;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.PieceType;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;

import java.util.List;

/**
 * Converts a {@link Move} (as decoded from a Stockfish UCI move string) into
 * SAN text for display in the report ("best move: Nf6", PV lines, etc).
 *
 * chesslib does not reliably expose SAN for a Move constructed outside of
 * PGN parsing, so this performs the conversion independently from a small
 * set of well-defined chesslib primitives (legalMoves, getPiece, doMove /
 * undoMove, isKingAttacked). If anything about the board/move shape doesn't
 * match what's expected here, it falls back to plain coordinate notation
 * (e.g. "e2e4") rather than throwing -- the report should never crash over
 * a cosmetic formatting detail.
 */
public final class SanFormatter {

    private SanFormatter() {
    }

    /**
     * @param board the position BEFORE the move is played (not mutated on return)
     * @param move  the move to render
     */
    public static String toSan(Board board, Move move) {
        try {
            Piece movingPiece = board.getPiece(move.getFrom());
            PieceType type = movingPiece.getPieceType();

            boolean isCastle = type == PieceType.KING
                    && Math.abs(move.getFrom().getFile().ordinal() - move.getTo().getFile().ordinal()) == 2;
            if (isCastle) {
                boolean kingSide = move.getTo().getFile().ordinal() > move.getFrom().getFile().ordinal();
                return applyAndSuffix(board, move, kingSide ? "O-O" : "O-O-O");
            }

            boolean isCapture = board.getPiece(move.getTo()) != Piece.NONE;
            if (type == PieceType.PAWN && !isCapture && move.getTo() == board.getEnPassant()) {
                isCapture = true;
            }

            StringBuilder sb = new StringBuilder();
            if (type == PieceType.PAWN) {
                if (isCapture) {
                    sb.append(fileChar(move.getFrom())).append('x');
                }
                sb.append(squareStr(move.getTo()));
                Piece promo = move.getPromotion();
                if (promo != null && promo != Piece.NONE) {
                    sb.append('=').append(Character.toUpperCase(promo.getFenSymbol().charAt(0)));
                }
            } else {
                sb.append(pieceLetter(type));
                sb.append(disambiguation(board, move, type));
                if (isCapture) sb.append('x');
                sb.append(squareStr(move.getTo()));
            }

            return applyAndSuffix(board, move, sb.toString());
        } catch (Exception e) {
            return fallback(move);
        }
    }

    /** Renders a sequence of moves (e.g. a principal variation) as a short SAN string. */
    public static String toSanSequence(Board startingPosition, List<Move> moves, int maxMoves) {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (Move m : moves) {
            if (count >= maxMoves) break;
            String san;
            try {
                san = toSan(startingPosition, m);
                startingPosition.doMove(m);
            } catch (Exception e) {
                san = fallback(m);
                break; // board state is now unreliable for further moves
            }
            if (count > 0) sb.append(' ');
            sb.append(san);
            count++;
        }
        return sb.toString();
    }

    private static String fallback(Move move) {
        return move.getFrom().toString().toLowerCase() + move.getTo().toString().toLowerCase();
    }

    private static String applyAndSuffix(Board board, Move move, String base) {
        board.doMove(move);
        try {
            boolean opponentInCheck = board.isKingAttacked();
            String suffix = "";
            if (opponentInCheck) {
                boolean noMoves = board.legalMoves().isEmpty();
                suffix = noMoves ? "#" : "+";
            }
            return base + suffix;
        } finally {
            board.undoMove();
        }
    }

    private static String disambiguation(Board board, Move move, PieceType type) {
        List<Move> legal = board.legalMoves();
        boolean fileAmbiguous = false, rankAmbiguous = false, anyAmbiguous = false;

        for (Move other : legal) {
            if (other.getTo() != move.getTo() || other.getFrom() == move.getFrom()) continue;
            Piece p = board.getPiece(other.getFrom());
            if (p == null || p.getPieceType() != type) continue;
            anyAmbiguous = true;
            if (other.getFrom().getFile() == move.getFrom().getFile()) fileAmbiguous = true;
            if (other.getFrom().getRank() == move.getFrom().getRank()) rankAmbiguous = true;
        }

        if (!anyAmbiguous) return "";
        if (!fileAmbiguous) return String.valueOf(fileChar(move.getFrom()));
        if (!rankAmbiguous) return String.valueOf(rankChar(move.getFrom()));
        return squareStr(move.getFrom());
    }

    private static String pieceLetter(PieceType type) {
        return switch (type) {
            case KNIGHT -> "N";
            case BISHOP -> "B";
            case ROOK -> "R";
            case QUEEN -> "Q";
            case KING -> "K";
            default -> "";
        };
    }

    private static char fileChar(Square sq) {
        return squareStr(sq).charAt(0);
    }

    private static char rankChar(Square sq) {
        return squareStr(sq).charAt(1);
    }

    private static String squareStr(Square sq) {
        return sq.toString().toLowerCase();
    }
}

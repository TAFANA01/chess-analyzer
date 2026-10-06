package com.chessanalyzer.board;

import com.chessanalyzer.model.Evaluation;
import com.chessanalyzer.model.MoveAnalysis;

/**
 * Everything the UI needs to know about the position at one point of an analysed game,
 * derived from the persisted {@link MoveAnalysis} data (never from a second chess model).
 *
 * <p>Ply 0 is the initial position; ply n is the position after the n-th analysed move.
 *
 * @param playedMove  the move that led to this position (null at ply 0)
 * @param nextMove    the move played next from this position (null at the final position)
 * @param evaluation  engine evaluation of this position, White's perspective (null if not analysed)
 * @param bestMoveUci engine's best move <em>from this position</em> (null at game end / unknown)
 * @param bestMoveSan same move in SAN
 * @param depth       engine depth for this position, 0 if not recorded (older saved analyses)
 * @param inCheck     the side to move is in check
 * @param checkmate   the side to move is checkmated
 */
public record PositionView(int ply, int plyCount, String fen,
                           MoveAnalysis playedMove, MoveAnalysis nextMove,
                           Evaluation evaluation, String bestMoveUci, String bestMoveSan,
                           int depth, boolean inCheck, boolean checkmate) {

    public boolean isInitial() {
        return ply == 0;
    }

    public boolean isFinal() {
        return ply == plyCount;
    }

    /** Label such as "27...Qd7" for the engine's alternative to {@code playedMove}; null if there is none. */
    public String alternativeLabel() {
        if (playedMove == null || playedMove.getBestMoveSan() == null) return null;
        return playedMove.getMoveNumber() + (playedMove.isWhiteToMove() ? "." : "...") + playedMove.getBestMoveSan();
    }
}

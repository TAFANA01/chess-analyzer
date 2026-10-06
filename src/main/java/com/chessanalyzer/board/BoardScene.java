package com.chessanalyzer.board;

import com.chessanalyzer.model.MoveClassification;

import java.util.List;

/**
 * A fully decided picture of the board: which FEN to draw, which squares to highlight and
 * which arrows to overlay. Produced by {@link BoardScenePlanner}; consumed by the renderer.
 *
 * @param lastFrom           origin square of the highlighted move, or {@link Squares#NONE}
 * @param lastTo             destination square of the highlighted move, or {@link Squares#NONE}
 * @param lastClassification classification of the highlighted move (tints the destination), may be null
 * @param checkSquare        king square to mark as in check, or {@link Squares#NONE}
 */
public record BoardScene(String fen, int lastFrom, int lastTo, MoveClassification lastClassification,
                         int checkSquare, List<BoardArrow> arrows) {

    public BoardScene {
        arrows = List.copyOf(arrows);
    }
}

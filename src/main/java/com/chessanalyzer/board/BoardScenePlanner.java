package com.chessanalyzer.board;

import com.chessanalyzer.model.MoveAnalysis;

import java.util.ArrayList;
import java.util.List;

/**
 * Decides what the board shows for a {@link PositionView}. Pure and deterministic.
 *
 * <ul>
 *   <li><b>Always</b>: the last move's origin/destination, and the king when it is in check.</li>
 *   <li><b>Inaccuracy / mistake / blunder</b>: an arrow for the engine's better move
 *       (taken from the stored analysis of the played move).</li>
 *   <li><b>Normal moves</b>: no arrow, to keep the board calm.</li>
 *   <li><b>On request</b> ({@link BoardOptions#showEngineSuggestion()}): an arrow for the engine's best
 *       move for the side to move.</li>
 *   <li><b>Compare mode</b> ({@link BoardOptions#compareBeforeMove()}): the position before the played move,
 *       with the played move and the engine's recommendation as arrows - the moves are drawn where they
 *       were actually decided, so origin squares always contain the right piece.</li>
 * </ul>
 */
public final class BoardScenePlanner {

    private BoardScenePlanner() {
    }

    /** True when the selected move is an inaccuracy/mistake/blunder, i.e. when the before-move comparison makes sense. */
    public static boolean isCompareApplicable(PositionView view) {
        MoveAnalysis played = view.playedMove();
        return played != null && played.getFenBefore() != null
                && ClassificationStyle.of(played.getClassification()).isProblem();
    }

    /** True when the board will show the position before the selected move. */
    public static boolean isComparing(PositionView view, BoardOptions options) {
        return options.compareBeforeMove() && isCompareApplicable(view);
    }

    public static BoardScene plan(PositionView view, BoardOptions options) {
        MoveAnalysis played = view.playedMove();
        ClassificationStyle style = played != null ? ClassificationStyle.of(played.getClassification()) : null;
        boolean problem = style != null && style.isProblem();

        boolean compare = isComparing(view, options);

        List<BoardArrow> arrows = new ArrayList<>();
        int checkSquare = Squares.NONE;
        String fen = view.fen();

        if (compare) {
            fen = played.getFenBefore();
            add(arrows, BoardArrow.fromUci(played.getUci(), BoardArrow.Kind.PLAYED));
            addBestAlternative(arrows, played);
        } else {
            if (problem) {
                addBestAlternative(arrows, played);
            }
            if (options.showEngineSuggestion() && view.bestMoveUci() != null) {
                BoardArrow suggestion = BoardArrow.fromUci(view.bestMoveUci(), BoardArrow.Kind.SUGGESTION);
                if (suggestion != null && !sameSquares(arrows, suggestion)) {
                    arrows.add(suggestion);
                }
            }
            if (view.inCheck()) {
                try {
                    checkSquare = FenPosition.parse(fen).kingSquare(FenPosition.parse(fen).whiteToMove());
                } catch (IllegalArgumentException e) {
                    checkSquare = Squares.NONE; // unreadable FEN: skip the decoration
                }
            }
        }

        int from = played != null ? Squares.uciFrom(played.getUci()) : Squares.NONE;
        int to = played != null ? Squares.uciTo(played.getUci()) : Squares.NONE;
        return new BoardScene(fen, from, to, played != null ? played.getClassification() : null, checkSquare, arrows);
    }

    private static void addBestAlternative(List<BoardArrow> arrows, MoveAnalysis played) {
        if (played.getBestMoveUci() == null || played.getBestMoveUci().equalsIgnoreCase(played.getUci())) {
            return; // the played move already was the engine's choice
        }
        add(arrows, BoardArrow.fromUci(played.getBestMoveUci(), BoardArrow.Kind.BEST_ALTERNATIVE));
    }

    private static void add(List<BoardArrow> arrows, BoardArrow arrow) {
        if (arrow != null) {
            arrows.add(arrow);
        }
    }

    private static boolean sameSquares(List<BoardArrow> arrows, BoardArrow candidate) {
        return arrows.stream().anyMatch(a -> a.from() == candidate.from() && a.to() == candidate.to());
    }
}

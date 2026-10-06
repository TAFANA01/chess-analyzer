package com.chessanalyzer.board;

/**
 * User-controlled display options.
 *
 * @param showEngineSuggestion draw the engine's best move for the side to move (off by default to avoid clutter)
 * @param compareBeforeMove    show the position <em>before</em> the selected move, with the played move and the
 *                             engine's recommendation as arrows
 */
public record BoardOptions(boolean showEngineSuggestion, boolean compareBeforeMove) {

    public static final BoardOptions DEFAULT = new BoardOptions(false, false);
}

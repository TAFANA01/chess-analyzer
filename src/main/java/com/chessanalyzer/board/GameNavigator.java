package com.chessanalyzer.board;

import com.chessanalyzer.model.Evaluation;
import com.chessanalyzer.model.GameAnalysis;
import com.chessanalyzer.model.MoveAnalysis;
import com.github.bhlangonijr.chesslib.Board;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;

import java.util.List;
import java.util.Objects;

/**
 * The single source of truth for "which position is being looked at".
 *
 * <p>Every view (board, move list, evaluation bar/panel, move information, graph) observes
 * {@link #positionProperty()} and only ever changes it through {@link #goTo(int)} and the
 * convenience methods below. The navigator never mutates the {@link GameAnalysis}, and it
 * never calls the engine: it reads the evaluations, best moves and FENs already stored in the
 * analysis.
 */
public final class GameNavigator {

    private final GameAnalysis analysis;
    private final List<MoveAnalysis> moves;
    private final ReadOnlyIntegerWrapper ply = new ReadOnlyIntegerWrapper(0);
    private final ReadOnlyObjectWrapper<PositionView> position = new ReadOnlyObjectWrapper<>();

    public GameNavigator(GameAnalysis analysis) {
        this.analysis = Objects.requireNonNull(analysis, "analysis");
        this.moves = List.copyOf(analysis.getMoves()); // snapshot: later edits cannot desynchronise the view
        position.set(viewAt(0));
    }

    public GameAnalysis analysis() {
        return analysis;
    }

    public List<MoveAnalysis> moves() {
        return moves;
    }

    public int plyCount() {
        return moves.size();
    }

    public int ply() {
        return ply.get();
    }

    public ReadOnlyIntegerProperty plyProperty() {
        return ply.getReadOnlyProperty();
    }

    public PositionView position() {
        return position.get();
    }

    public ReadOnlyObjectProperty<PositionView> positionProperty() {
        return position.getReadOnlyProperty();
    }

    // ------------------------------------------------------------------ navigation

    /** Jumps to a position; out-of-range values are clamped to [0, plyCount]. */
    public void goTo(int target) {
        int clamped = Math.max(0, Math.min(moves.size(), target));
        if (clamped == ply.get()) {
            return;
        }
        PositionView view = viewAt(clamped);
        // Publish the new position before the ply so ply-listeners always see a matching position.
        position.set(view);
        ply.set(clamped);
    }

    public void first() {
        goTo(0);
    }

    public void previous() {
        goTo(ply.get() - 1);
    }

    public void next() {
        goTo(ply.get() + 1);
    }

    public void last() {
        goTo(moves.size());
    }

    public boolean atStart() {
        return ply.get() == 0;
    }

    public boolean atEnd() {
        return ply.get() == moves.size();
    }

    // ------------------------------------------------------------------ position derivation

    /**
     * The derived view of any position of this game (does not change the current position).
     * Used e.g. to describe the position <em>before</em> the selected move in comparison mode.
     *
     * @throws IndexOutOfBoundsException if {@code target} is outside [0, plyCount]
     */
    public PositionView viewAt(int target) {
        if (target < 0 || target > moves.size()) {
            throw new IndexOutOfBoundsException("ply " + target + " not in [0, " + moves.size() + "]");
        }
        MoveAnalysis played = target > 0 ? moves.get(target - 1) : null;
        MoveAnalysis next = target < moves.size() ? moves.get(target) : null;

        String fen = fenAt(target);
        Evaluation eval = target == 0
                ? (next != null ? next.getEvalBefore() : null)
                : played.getEvalAfter();
        String bestUci = next != null ? next.getBestMoveUci() : null;
        String bestSan = next != null ? next.getBestMoveSan() : null;
        int depth = next != null ? next.getEngineDepth() : (played != null ? played.getEngineDepthAfter() : 0);

        boolean inCheck = false;
        boolean checkmate = false;
        try {
            Board board = new Board();
            board.loadFromFen(fen);
            inCheck = board.isKingAttacked();
            checkmate = board.isMated();
        } catch (RuntimeException e) {
            // A FEN chesslib cannot load only disables check/mate decoration; the board still renders from the FEN.
        }
        return new PositionView(target, moves.size(), fen, played, next, eval, bestUci, bestSan,
                depth, inCheck, checkmate);
    }

    private String fenAt(int target) {
        if (target > 0) {
            return moves.get(target - 1).getFenAfter();
        }
        if (!moves.isEmpty()) {
            return moves.get(0).getFenBefore();
        }
        String start = analysis.getGame() != null ? analysis.getGame().getStartFen() : null;
        return (start != null && !start.isBlank()) ? start : FenPosition.START_FEN;
    }
}

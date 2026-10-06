package com.chessanalyzer.util;

import com.chessanalyzer.model.Evaluation;

/**
 * Shared helpers for working with engine evaluations.
 * All Evaluation objects in this app are stored from White's perspective;
 * this class provides the perspective flips and loss math needed by the
 * classification/accuracy services.
 */
public final class EvaluationUtil {

    private EvaluationUtil() {
    }

    /**
     * Evaluation loss (in centipawns) suffered by the side who just moved,
     * comparing the position's evaluation before the move (best case, from
     * the mover's perspective) to the evaluation actually reached after the
     * move was played.
     *
     * Both inputs are in White's perspective; the result is always >= 0
     * (0 = no loss, i.e. the played move matched or exceeded the best line).
     */
    public static double lossForMover(Evaluation beforeWhitePerspective,
                                       Evaluation afterWhitePerspective,
                                       boolean whiteToMove) {
        double before = fromMoverPerspective(beforeWhitePerspective, whiteToMove);
        double after = fromMoverPerspective(afterWhitePerspective, whiteToMove);
        // Clamp both sides so a mate score contributes a bounded, meaningful loss
        // (e.g. +3.0 -> being mated = 1300cp) instead of an arbitrary 10 000-point swing.
        double loss = clampForAccuracy(before) - clampForAccuracy(after);
        return Math.max(0, loss);
    }

    /** Maximum |centipawns| considered when computing loss/accuracy; mates are clamped to this. */
    public static final double EVAL_CLAMP_CP = 1000;

    /** Converts a White-perspective evaluation into the moving side's perspective. */
    public static double fromMoverPerspective(Evaluation evalWhitePerspective, boolean whiteToMove) {
        double v = evalWhitePerspective.asComparable();
        return whiteToMove ? v : -v;
    }

    /** Clamp a centipawn evaluation used for accuracy math so mates don't blow up the curve. */
    public static double clampForAccuracy(double centipawns) {
        return Math.max(-EVAL_CLAMP_CP, Math.min(EVAL_CLAMP_CP, centipawns));
    }

    /** Human phrase describing an advantage size, from White's perspective. */
    public static String describeAdvantage(Evaluation evalWhitePerspective) {
        if (evalWhitePerspective.isMate()) {
            return evalWhitePerspective.mateSign() > 0 ? "White has a forced mate" : "Black has a forced mate";
        }
        int cp = evalWhitePerspective.getCentipawns();
        double pawns = cp / 100.0;
        if (Math.abs(pawns) < 0.4) return "the position is roughly equal";
        String side = pawns > 0 ? "White" : "Black";
        double abs = Math.abs(pawns);
        if (abs < 1.0) return side + " has a slight edge";
        if (abs < 2.0) return side + " has a clear advantage";
        if (abs < 4.0) return side + " has a significant advantage";
        return side + " has a decisive advantage";
    }
}

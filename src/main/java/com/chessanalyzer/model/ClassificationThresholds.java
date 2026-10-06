package com.chessanalyzer.model;

/**
 * Centipawn-loss boundaries used to classify a move. Each value is the
 * <em>maximum</em> loss (inclusive) that still earns the given class; anything
 * above {@link #mistakeMaxLoss()} is a blunder.
 *
 * <p>Loss is measured from the mover's perspective against the engine's
 * evaluation of the position before the move, with evaluations clamped to
 * &plusmn;{@code EvaluationUtil.EVAL_CLAMP_CP} so mate scores stay bounded.
 *
 * <p>The defaults are a documented convention, not an engine truth: they sit
 * close to the commonly used 20/60/120/250 cp ladder and can be overridden
 * by constructing a custom instance.
 *
 * @param excellentMaxLoss   non-engine-best move still "Excellent" up to this loss
 * @param goodMaxLoss        "Good" up to this loss
 * @param inaccuracyMaxLoss  "Inaccuracy" up to this loss
 * @param mistakeMaxLoss     "Mistake" up to this loss; above is "Blunder"
 * @param decisiveEvalCp     when the mover already had |eval| above this, loss is scaled down
 * @param decisiveScaling    scaling factor (0..1] applied to loss in already-decisive positions
 * @param brilliantMinSacrificeCp minimum material sacrificed (cp) for a "Brilliant" move
 * @param brilliantMaxLoss   a Brilliant move may lose at most this much vs. the best move
 */
public record ClassificationThresholds(double excellentMaxLoss,
                                       double goodMaxLoss,
                                       double inaccuracyMaxLoss,
                                       double mistakeMaxLoss,
                                       double decisiveEvalCp,
                                       double decisiveScaling,
                                       double brilliantMinSacrificeCp,
                                       double brilliantMaxLoss) {

    public ClassificationThresholds {
        if (!(excellentMaxLoss <= goodMaxLoss && goodMaxLoss <= inaccuracyMaxLoss
                && inaccuracyMaxLoss <= mistakeMaxLoss)) {
            throw new IllegalArgumentException("Thresholds must be non-decreasing");
        }
        if (decisiveScaling <= 0 || decisiveScaling > 1) {
            throw new IllegalArgumentException("decisiveScaling must be in (0, 1]");
        }
    }

    public static ClassificationThresholds defaults() {
        return new ClassificationThresholds(20, 60, 120, 250, 600, 0.5, 200, 20);
    }
}

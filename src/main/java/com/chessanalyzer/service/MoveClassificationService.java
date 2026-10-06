package com.chessanalyzer.service;

import com.chessanalyzer.model.ClassificationThresholds;
import com.chessanalyzer.model.Evaluation;
import com.chessanalyzer.model.MoveClassification;
import com.chessanalyzer.util.EvaluationUtil;

/**
 * Classifies a played move from the evaluation it gave up relative to the
 * engine's best line.
 *
 * <h3>Logic</h3>
 * <ol>
 *   <li>If the played move is the engine's top choice it is {@code BEST}.</li>
 *   <li>Otherwise the loss (centipawns, mover's perspective, clamped) is
 *       compared against {@link ClassificationThresholds}. A non-top move can
 *       never be {@code BEST} - at most {@code EXCELLENT} - because loss values
 *       from two independent searches contain noise.</li>
 *   <li>When the mover already had a decisive advantage (|eval| above
 *       {@code decisiveEvalCp}) the loss is scaled down: surrendering 150cp
 *       of +8.00 matters much less than 150cp of an equal position.</li>
 *   <li>{@code BRILLIANT} is awarded only when {@link #classify(Evaluation, Evaluation,
 *       boolean, String, String, double)} is told the move sacrificed material
 *       (see {@code materialSacrificedCp}), it is (near) best, and the mover is
 *       not already clearly losing or hopelessly winning.</li>
 * </ol>
 */
public class MoveClassificationService {

    private final ClassificationThresholds thresholds;

    public MoveClassificationService() {
        this(ClassificationThresholds.defaults());
    }

    public MoveClassificationService(ClassificationThresholds thresholds) {
        this.thresholds = thresholds;
    }

    public ClassificationThresholds thresholds() {
        return thresholds;
    }

    /** Backward-compatible overload: no sacrifice information, so never BRILLIANT. */
    public MoveClassification classify(Evaluation evalBeforeWhitePerspective,
                                       Evaluation evalAfterWhitePerspective,
                                       boolean whiteToMove,
                                       String playedMoveUci,
                                       String bestMoveUci) {
        return classify(evalBeforeWhitePerspective, evalAfterWhitePerspective, whiteToMove,
                playedMoveUci, bestMoveUci, 0);
    }

    /**
     * @param materialSacrificedCp net material (in centipawns) the mover gives up by this move
     *                             on the first plies of the engine's line; 0 if unknown/none.
     */
    public MoveClassification classify(Evaluation evalBeforeWhitePerspective,
                                       Evaluation evalAfterWhitePerspective,
                                       boolean whiteToMove,
                                       String playedMoveUci,
                                       String bestMoveUci,
                                       double materialSacrificedCp) {

        double moverEvalBefore = EvaluationUtil.fromMoverPerspective(evalBeforeWhitePerspective, whiteToMove);
        double moverEvalAfter = EvaluationUtil.fromMoverPerspective(evalAfterWhitePerspective, whiteToMove);
        boolean playedBest = bestMoveUci != null && bestMoveUci.equalsIgnoreCase(playedMoveUci);

        double rawLoss = playedBest ? 0
                : EvaluationUtil.lossForMover(evalBeforeWhitePerspective, evalAfterWhitePerspective, whiteToMove);

        double effectiveLoss = Math.abs(moverEvalBefore) > thresholds.decisiveEvalCp()
                ? rawLoss * thresholds.decisiveScaling() : rawLoss;

        boolean notAlreadyDecided = moverEvalBefore > -300 && moverEvalBefore < thresholds.decisiveEvalCp();
        if (materialSacrificedCp >= thresholds.brilliantMinSacrificeCp()
                && rawLoss <= thresholds.brilliantMaxLoss()
                && moverEvalAfter >= -100
                && notAlreadyDecided) {
            return MoveClassification.BRILLIANT;
        }

        if (playedBest) return MoveClassification.BEST;
        if (effectiveLoss <= thresholds.excellentMaxLoss()) return MoveClassification.EXCELLENT;
        if (effectiveLoss <= thresholds.goodMaxLoss()) return MoveClassification.GOOD;
        if (effectiveLoss <= thresholds.inaccuracyMaxLoss()) return MoveClassification.INACCURACY;
        if (effectiveLoss <= thresholds.mistakeMaxLoss()) return MoveClassification.MISTAKE;
        return MoveClassification.BLUNDER;
    }
}

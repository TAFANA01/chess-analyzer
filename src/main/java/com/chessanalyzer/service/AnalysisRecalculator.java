package com.chessanalyzer.service;

import com.chessanalyzer.model.ClassificationThresholds;
import com.chessanalyzer.model.GameAnalysis;
import com.chessanalyzer.model.MoveAnalysis;
import com.chessanalyzer.util.EvaluationUtil;

/**
 * Recomputes loss, classification and every derived statistic of an existing
 * {@link GameAnalysis} from the engine evaluations already stored in it.
 * No engine is started. Used when migrating old saved files and when the
 * user changes classification thresholds.
 */
public class AnalysisRecalculator {

    private final AnalysisService analysisService = new AnalysisService();

    public void recalculate(GameAnalysis analysis) {
        recalculate(analysis, ClassificationThresholds.defaults());
    }

    public void recalculate(GameAnalysis analysis, ClassificationThresholds thresholds) {
        MoveClassificationService classifier = new MoveClassificationService(thresholds);
        for (MoveAnalysis m : analysis.getMoves()) {
            if (m.getEvalBefore() == null || m.getEvalAfter() == null) {
                continue;
            }
            boolean playedBest = m.getUci() != null && m.getUci().equalsIgnoreCase(m.getBestMoveUci());
            double loss = playedBest ? 0
                    : EvaluationUtil.lossForMover(m.getEvalBefore(), m.getEvalAfter(), m.isWhiteToMove());
            m.setEvalLossCentipawns(loss);
            m.setClassification(classifier.classify(m.getEvalBefore(), m.getEvalAfter(),
                    m.isWhiteToMove(), m.getUci(), m.getBestMoveUci()));
        }
        analysisService.finalizeAnalysis(analysis);
    }
}

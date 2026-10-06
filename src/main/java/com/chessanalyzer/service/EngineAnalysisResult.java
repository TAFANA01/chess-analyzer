package com.chessanalyzer.service;

import com.chessanalyzer.model.Evaluation;

import java.util.List;

/**
 * Result of analyzing one position with Stockfish.
 * The evaluation is always normalized to White's perspective.
 */
public class EngineAnalysisResult {

    private final Evaluation evaluation;
    private final String bestMoveUci;
    private final List<String> principalVariationUci;
    private final int depthReached;

    public EngineAnalysisResult(Evaluation evaluation, String bestMoveUci,
                                 List<String> principalVariationUci, int depthReached) {
        this.evaluation = evaluation;
        this.bestMoveUci = bestMoveUci;
        this.principalVariationUci = principalVariationUci;
        this.depthReached = depthReached;
    }

    public Evaluation getEvaluation() {
        return evaluation;
    }

    public String getBestMoveUci() {
        return bestMoveUci;
    }

    public List<String> getPrincipalVariationUci() {
        return principalVariationUci;
    }

    public int getDepthReached() {
        return depthReached;
    }
}

package com.chessanalyzer.service;

import com.chessanalyzer.model.MoveAnalysis;
import com.chessanalyzer.util.EvaluationUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Computes per-player accuracy from evaluation swings, deliberately NOT using
 * a plain "100 - average centipawn loss" formula.
 *
 * Approach:
 *  1. Convert each side's evaluation before/after a move into a "win
 *     probability" using a logistic curve (the standard chess-engine
 *     centipawn-to-win% mapping).
 *  2. A move's individual accuracy is a decaying exponential function of how
 *     much win probability it gave up -- small drops barely register, large
 *     drops crater the score, so mistakes progressively reduce accuracy
 *     rather than being averaged away.
 *  3. The game's overall accuracy blends the arithmetic mean of per-move
 *     scores with their harmonic mean; the harmonic mean is pulled down
 *     hard by any very low outlier (a blunder), which the arithmetic mean
 *     alone would dilute across many good moves.
 *
 * Kept isolated behind this class so the formula can be swapped or tuned
 * later without touching AnalysisService or the report generation.
 */
public class AccuracyService {

    private static final double WIN_PERCENT_SCALE = 0.00368208;
    private static final double ACCURACY_CURVE_A = 103.1668;
    private static final double ACCURACY_CURVE_K = 0.04354;
    private static final double ACCURACY_CURVE_OFFSET = 3.1668;

    /** Converts a centipawn evaluation (from one player's own perspective) into a 0-100 win probability. */
    public double winPercent(double centipawnsFromPlayerPerspective) {
        double cp = EvaluationUtil.clampForAccuracy(centipawnsFromPlayerPerspective);
        return 50 + 50 * (2.0 / (1 + Math.exp(-WIN_PERCENT_SCALE * cp)) - 1);
    }

    /** Accuracy contribution of a single move, given the win% swing it caused (always >= 0 drop). */
    public double moveAccuracy(double winPercentBefore, double winPercentAfter) {
        double drop = Math.max(0, winPercentBefore - winPercentAfter);
        double acc = ACCURACY_CURVE_A * Math.exp(-ACCURACY_CURVE_K * drop) - ACCURACY_CURVE_OFFSET;
        return clamp(acc, 0, 100);
    }

    private double aggregate(List<Double> perMoveAccuracies) {
        if (perMoveAccuracies.isEmpty()) return 100.0;

        double arithmeticMean = perMoveAccuracies.stream().mapToDouble(Double::doubleValue).average().orElse(100);

        double reciprocalSum = 0;
        for (double a : perMoveAccuracies) {
            reciprocalSum += 1.0 / Math.max(a, 1.0);
        }
        double harmonicMean = perMoveAccuracies.size() / reciprocalSum;

        // Blend: harmonic mean dominates enough to make blunders matter,
        // arithmetic mean keeps a single bad move from being catastrophic.
        return clamp(0.5 * harmonicMean + 0.5 * arithmeticMean, 0, 100);
    }

    /**
     * Computes final White/Black accuracy for a fully-analyzed game.
     */
    public AccuracyResult computeForGame(List<MoveAnalysis> moves) {
        List<Double> whiteScores = new ArrayList<>();
        List<Double> blackScores = new ArrayList<>();

        for (MoveAnalysis m : moves) {
            if (m.getEvalBefore() == null || m.getEvalAfter() == null) continue;

            boolean white = m.isWhiteToMove();
            double beforeMoverPerspective = EvaluationUtil.fromMoverPerspective(m.getEvalBefore(), white);
            double afterMoverPerspective = EvaluationUtil.fromMoverPerspective(m.getEvalAfter(), white);

            double winBefore = winPercent(beforeMoverPerspective);
            double winAfter = winPercent(afterMoverPerspective);
            double acc = moveAccuracy(winBefore, winAfter);

            (white ? whiteScores : blackScores).add(acc);
        }

        return new AccuracyResult(aggregate(whiteScores), aggregate(blackScores));
    }

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}

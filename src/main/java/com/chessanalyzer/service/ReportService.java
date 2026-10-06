package com.chessanalyzer.service;

import com.chessanalyzer.model.CriticalMoment;
import com.chessanalyzer.model.FinalReport;
import com.chessanalyzer.model.GameAnalysis;
import com.chessanalyzer.model.GamePhase;
import com.chessanalyzer.model.MoveAnalysis;
import com.chessanalyzer.model.MoveClassification;
import com.chessanalyzer.model.PhaseStats;
import com.chessanalyzer.util.EvaluationUtil;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Builds the final, user-facing report from a fully-analyzed game: selects
 * the handful of critical moments worth showing, and writes the narrative
 * sections (evaluation summary, strong/weak points, recommendations, final
 * assessment).
 */
public class ReportService {

    private static final int MAX_CRITICAL_MOMENTS = 8;

    public void generateReport(GameAnalysis analysis) {
        selectCriticalMoments(analysis);

        FinalReport report = new FinalReport();
        writeEvaluationSummary(analysis, report);
        writeStrongAndWeakPoints(analysis, report);
        writeRecommendations(analysis, report);
        writeFinalAssessment(analysis, report);

        analysis.setFinalReport(report);
    }

    // --- Critical moments ---

    private void selectCriticalMoments(GameAnalysis analysis) {
        List<MoveAnalysis> moves = analysis.getMoves();

        List<MoveAnalysis> mistakesAndBlunders = moves.stream()
                .filter(m -> m.getClassification() == MoveClassification.MISTAKE
                        || m.getClassification() == MoveClassification.BLUNDER)
                .sorted(Comparator.comparingDouble(MoveAnalysis::getEvalLossCentipawns).reversed())
                .toList();

        List<MoveAnalysis> selected;
        if (mistakesAndBlunders.size() >= 3) {
            selected = mistakesAndBlunders.stream().limit(MAX_CRITICAL_MOMENTS).toList();
        } else {
            // Not many hard mistakes: round out with the most significant inaccuracies
            // so the report still has something meaningful to show.
            List<MoveAnalysis> inaccuracies = moves.stream()
                    .filter(m -> m.getClassification() == MoveClassification.INACCURACY)
                    .sorted(Comparator.comparingDouble(MoveAnalysis::getEvalLossCentipawns).reversed())
                    .limit(MAX_CRITICAL_MOMENTS - mistakesAndBlunders.size())
                    .toList();
            selected = new java.util.ArrayList<>(mistakesAndBlunders);
            selected.addAll(inaccuracies);
        }

        // Restore chronological order for display.
        List<MoveAnalysis> chronological = moves.stream()
                .filter(selected::contains)
                .toList();

        for (MoveAnalysis m : chronological) {
            m.setCriticalMoment(true);
            analysis.getCriticalMoments().add(CriticalMoment.from(m));
        }
    }

    // --- Evaluation summary ---

    private void writeEvaluationSummary(GameAnalysis analysis, FinalReport report) {
        List<MoveAnalysis> moves = analysis.getMoves();
        if (moves.isEmpty()) return;

        MoveAnalysis openingEnd = lastMoveInPhase(moves, GamePhase.OPENING).orElse(moves.get(0));
        report.setOpeningEvaluationSummary(capitalize(EvaluationUtil.describeAdvantage(openingEnd.getEvalAfter())));

        Optional<MoveAnalysis> middlegameEnd = lastMoveInPhase(moves, GamePhase.MIDDLEGAME);
        middlegameEnd.ifPresent(m ->
                report.setMiddlegameEvaluationSummary(capitalize(EvaluationUtil.describeAdvantage(m.getEvalAfter()))));

        // The single biggest evaluation swing in the game, in the losing side's favor for the opponent.
        Optional<MoveAnalysis> biggestSwing = moves.stream()
                .max(Comparator.comparingDouble(MoveAnalysis::getEvalLossCentipawns));

        biggestSwing.ifPresent(m -> {
            report.setCriticalMomentSummary("Move " + m.getMoveNumber());
            report.setPostCriticalMomentSummary(capitalize(EvaluationUtil.describeAdvantage(m.getEvalAfter())));
        });

        MoveAnalysis last = moves.get(moves.size() - 1);
        report.setFinalEvaluationSummary(capitalize(EvaluationUtil.describeAdvantage(last.getEvalAfter())));
    }

    private Optional<MoveAnalysis> lastMoveInPhase(List<MoveAnalysis> moves, GamePhase phase) {
        MoveAnalysis last = null;
        for (MoveAnalysis m : moves) {
            if (m.getPhase() == phase) last = m;
        }
        return Optional.ofNullable(last);
    }

    // --- Strong / weak points ---

    private void writeStrongAndWeakPoints(GameAnalysis analysis, FinalReport report) {
        int whiteBlunders = analysis.getMoveStatistics().getCount(true, MoveClassification.BLUNDER);
        int blackBlunders = analysis.getMoveStatistics().getCount(false, MoveClassification.BLUNDER);
        int whiteMistakes = analysis.getMoveStatistics().getCount(true, MoveClassification.MISTAKE);
        int blackMistakes = analysis.getMoveStatistics().getCount(false, MoveClassification.MISTAKE);

        for (PhaseStats ps : analysis.getPhaseStats()) {
            if (!ps.isPresent()) continue;
            if (ps.getPhase() == GamePhase.OPENING && (ps.getWhiteAccuracy() >= 85 || ps.getBlackAccuracy() >= 85)) {
                report.getStrongPoints().add("Good opening development");
            }
            if (ps.getPhase() == GamePhase.MIDDLEGAME && (ps.getWhiteAccuracy() >= 85 || ps.getBlackAccuracy() >= 85)) {
                report.getStrongPoints().add("Strong play in the middlegame");
            }
            if (ps.getPhase() == GamePhase.ENDGAME && (ps.getWhiteAccuracy() >= 85 || ps.getBlackAccuracy() >= 85)) {
                report.getStrongPoints().add("Good conversion in the endgame");
            }
        }
        if (whiteBlunders == 0 && blackBlunders == 0) {
            report.getStrongPoints().add("No outright blunders in the game");
        }
        if (report.getStrongPoints().isEmpty()) {
            report.getStrongPoints().add("Consistent decision-making without major collapses");
        }

        if (whiteBlunders + blackBlunders > 0) {
            report.getWeakPoints().add("Missed tactical opportunities leading to blunders");
        }
        if (whiteMistakes + blackMistakes > 0) {
            report.getWeakPoints().add("Calculation errors in critical positions");
        }
        boolean endgamePresent = analysis.getPhaseStats().stream()
                .anyMatch(p -> p.getPhase() == GamePhase.ENDGAME && p.isPresent());
        if (endgamePresent) {
            double endgameAcc = analysis.getPhaseStats().stream()
                    .filter(p -> p.getPhase() == GamePhase.ENDGAME)
                    .findFirst().map(p -> Math.min(p.getWhiteAccuracy(), p.getBlackAccuracy())).orElse(100.0);
            if (endgameAcc < 75) {
                report.getWeakPoints().add("Technique could be sharper in the endgame");
            }
        }
        if (report.getWeakPoints().isEmpty()) {
            report.getWeakPoints().add("No significant recurring weaknesses identified");
        }
    }

    // --- Recommendations ---

    private void writeRecommendations(GameAnalysis analysis, FinalReport report) {
        int totalBlunders = analysis.getMoveStatistics().getCount(true, MoveClassification.BLUNDER)
                + analysis.getMoveStatistics().getCount(false, MoveClassification.BLUNDER);
        int totalMistakes = analysis.getMoveStatistics().getCount(true, MoveClassification.MISTAKE)
                + analysis.getMoveStatistics().getCount(false, MoveClassification.MISTAKE);

        if (totalBlunders > 0) {
            report.getRecommendations().add("Check forcing moves (captures, checks, threats) before committing to a move.");
        }
        if (totalMistakes > 0) {
            report.getRecommendations().add("Pay more attention to opponent tactical threats before advancing your own plan.");
        }
        report.getRecommendations().add("Improve calculation depth in complex middlegame positions.");

        boolean endgameWeak = analysis.getPhaseStats().stream()
                .anyMatch(p -> p.getPhase() == GamePhase.ENDGAME && p.isPresent()
                        && Math.min(p.getWhiteAccuracy(), p.getBlackAccuracy()) < 75);
        if (endgameWeak) {
            report.getRecommendations().add("Review fundamental endgame technique (king activity, opposition, conversion of material advantages).");
        }
    }

    // --- Final assessment ---

    private void writeFinalAssessment(GameAnalysis analysis, FinalReport report) {
        StringBuilder sb = new StringBuilder();

        double openingAvg = firstPhaseAvgAccuracy(analysis, GamePhase.OPENING);
        if (openingAvg >= 85) {
            sb.append("The game was accurate in the opening. ");
        } else if (openingAvg > 0) {
            sb.append("The opening had some imprecisions on both sides. ");
        }

        if (!analysis.getCriticalMoments().isEmpty()) {
            CriticalMoment turning = analysis.getCriticalMoments().stream()
                    .max(Comparator.comparing(cm -> cm.getClassification().ordinal()))
                    .orElse(analysis.getCriticalMoments().get(0));
            sb.append("The main turning point occurred around ").append(turning.getMoveLabel())
                    .append(", where a ").append(turning.getClassification().label().toLowerCase())
                    .append(" significantly changed the evaluation. ");
        }

        String stronger = analysis.getWhiteAccuracy() >= analysis.getBlackAccuracy() ? "White" : "Black";
        sb.append(stronger).append(" played the more accurate game overall, with an accuracy of ")
                .append(String.format("%.1f%%", Math.max(analysis.getWhiteAccuracy(), analysis.getBlackAccuracy())))
                .append(" against ")
                .append(String.format("%.1f%%", Math.min(analysis.getWhiteAccuracy(), analysis.getBlackAccuracy())))
                .append(".");

        report.setFinalAssessment(sb.toString());
    }

    private double firstPhaseAvgAccuracy(GameAnalysis analysis, GamePhase phase) {
        return analysis.getPhaseStats().stream()
                .filter(p -> p.getPhase() == phase && p.isPresent())
                .findFirst()
                .map(p -> (p.getWhiteAccuracy() + p.getBlackAccuracy()) / 2.0)
                .orElse(0.0);
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}

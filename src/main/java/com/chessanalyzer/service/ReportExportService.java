package com.chessanalyzer.service;

import com.chessanalyzer.model.ChessGame;
import com.chessanalyzer.model.CriticalMoment;
import com.chessanalyzer.model.FinalReport;
import com.chessanalyzer.model.GameAnalysis;
import com.chessanalyzer.model.MoveClassification;
import com.chessanalyzer.model.PhaseStats;
import com.chessanalyzer.util.FileUtil;
import com.chessanalyzer.util.JsonUtil;

import java.io.File;
import java.io.IOException;

/**
 * Exports a completed analysis as a plain-text report or as raw JSON.
 * The TXT structure mirrors the report sections in the spec closely enough
 * that it could be re-flowed into a PDF layout in a future version.
 */
public class ReportExportService {

    public void exportJson(GameAnalysis analysis, File file) throws IOException {
        JsonUtil.writeToFile(analysis, file);
    }

    public void exportTxt(GameAnalysis analysis, File file) throws IOException {
        FileUtil.writeTextFile(file, buildTxtReport(analysis));
    }

    public String buildTxtReport(GameAnalysis analysis) {
        ChessGame game = analysis.getGame();
        FinalReport report = analysis.getFinalReport();
        StringBuilder sb = new StringBuilder();

        sb.append("CHESS GAME ANALYZER - FINAL REPORT\n");
        sb.append("=".repeat(40)).append("\n\n");

        sb.append("GAME OVERVIEW\n").append("-".repeat(40)).append("\n");
        sb.append("White:   ").append(game.getWhite()).append("\n");
        sb.append("Black:   ").append(game.getBlack()).append("\n");
        sb.append("Result:  ").append(game.getResult()).append("\n");
        sb.append("Opening: ").append(game.getOpening().isBlank() ? "-" : game.getOpening()).append("\n");
        sb.append("Moves:   ").append(game.getMoveCount()).append("\n\n");

        sb.append("ACCURACY\n").append("-".repeat(40)).append("\n");
        sb.append(String.format("White: %.1f%%%n", analysis.getWhiteAccuracy()));
        sb.append(String.format("Black: %.1f%%%n%n", analysis.getBlackAccuracy()));

        sb.append("MOVE STATISTICS\n").append("-".repeat(40)).append("\n");
        sb.append(String.format("%-14s %8s %8s%n", "", "White", "Black"));
        for (MoveClassification c : MoveClassification.values()) {
            sb.append(String.format("%-14s %8d %8d%n", c.label(),
                    analysis.getMoveStatistics().getCount(true, c),
                    analysis.getMoveStatistics().getCount(false, c)));
        }
        sb.append("\n");

        sb.append("GAME PHASES\n").append("-".repeat(40)).append("\n");
        for (PhaseStats ps : analysis.getPhaseStats()) {
            if (!ps.isPresent()) continue;
            sb.append(ps.getPhase()).append("\n");
            sb.append(String.format("  White: %.1f%%%n", ps.getWhiteAccuracy()));
            sb.append(String.format("  Black: %.1f%%%n", ps.getBlackAccuracy()));
        }
        sb.append("\n");

        sb.append("CRITICAL MOMENTS\n").append("-".repeat(40)).append("\n");
        if (analysis.getCriticalMoments().isEmpty()) {
            sb.append("No critical moments detected.\n");
        }
        for (CriticalMoment cm : analysis.getCriticalMoments()) {
            sb.append(cm.getMoveLabel()).append("\n");
            sb.append(cm.getClassification().label()).append("\n");
            if (cm.getEvalBefore() != null && cm.getEvalAfter() != null) {
                sb.append("Evaluation: ").append(cm.getEvalBefore().format())
                        .append(" -> ").append(cm.getEvalAfter().format()).append("\n");
            }
            if (cm.getBestMoveSan() != null) {
                sb.append("Best move: ").append(cm.getBestMoveSan()).append("\n");
            }
            if (cm.getReasonCategory() != null) {
                sb.append("Reason: ").append(cm.getReasonCategory()).append("\n");
            }
            if (cm.getComment() != null) {
                sb.append(cm.getComment()).append("\n");
            }
            sb.append("\n");
        }

        if (report != null) {
            sb.append("STRONG POINTS\n").append("-".repeat(40)).append("\n");
            for (String s : report.getStrongPoints()) sb.append("- ").append(s).append("\n");
            sb.append("\n");

            sb.append("WEAK POINTS\n").append("-".repeat(40)).append("\n");
            for (String s : report.getWeakPoints()) sb.append("- ").append(s).append("\n");
            sb.append("\n");

            sb.append("IMPROVEMENT RECOMMENDATIONS\n").append("-".repeat(40)).append("\n");
            int i = 1;
            for (String s : report.getRecommendations()) sb.append(i++).append(". ").append(s).append("\n");
            sb.append("\n");

            sb.append("FINAL ASSESSMENT\n").append("-".repeat(40)).append("\n");
            sb.append(report.getFinalAssessment()).append("\n");
        }

        return sb.toString();
    }
}

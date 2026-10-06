package com.chessanalyzer.controller;

import com.chessanalyzer.Main;
import com.chessanalyzer.model.ChessGame;
import com.chessanalyzer.model.CriticalMoment;
import com.chessanalyzer.model.FinalReport;
import com.chessanalyzer.model.GameAnalysis;
import com.chessanalyzer.model.MoveClassification;
import com.chessanalyzer.model.PhaseStats;
import com.chessanalyzer.service.ReportExportService;
import com.chessanalyzer.ui.AnalysisBoardView;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;

/**
 * Renders the complete final report described in the spec (sections 17 and
 * 20): game overview, accuracy, move statistics, game phases, critical
 * moments, strong/weak points, improvement tips, and final assessment.
 *
 * The report's content is variable-length (a different number of critical
 * moments, phases, etc. per game), so the sections are built programmatically
 * into a VBox rather than laid out statically in FXML.
 */
public class ReportController {

    @FXML private VBox contentBox;

    private final ReportExportService exportService = new ReportExportService();
    private GameAnalysis analysis;

    public void showReport(GameAnalysis analysis) {
        this.analysis = analysis;
        contentBox.getChildren().clear();

        addGameOverview();
        addAccuracy();
        addMoveStatistics();
        addGamePhases();
        addCriticalMoments();

        FinalReport report = analysis.getFinalReport();
        if (report != null) {
            addBulletSection("STRONG POINTS", report.getStrongPoints(), false);
            addBulletSection("WEAK POINTS", report.getWeakPoints(), false);
            addBulletSection("IMPROVEMENT TIPS", report.getRecommendations(), true);
            addFinalAssessment(report);
        }
    }

    private void addGameOverview() {
        addHeading("GAME OVERVIEW");
        ChessGame game = analysis.getGame();
        contentBox.getChildren().add(new Label("White:   " + game.getWhite()));
        contentBox.getChildren().add(new Label("Black:   " + game.getBlack()));
        contentBox.getChildren().add(new Label("Result:  " + game.getResult()));
        contentBox.getChildren().add(new Label("Opening: " + (game.getOpening() == null || game.getOpening().isBlank() ? "-" : game.getOpening())));
        contentBox.getChildren().add(new Label("Moves:   " + game.getMoveCount()));
    }

    private void addAccuracy() {
        addHeading("ACCURACY");
        contentBox.getChildren().add(new Label(String.format("White: %.1f%%", analysis.getWhiteAccuracy())));
        contentBox.getChildren().add(new Label(String.format("Black: %.1f%%", analysis.getBlackAccuracy())));
    }

    private void addMoveStatistics() {
        addHeading("MOVE STATISTICS");
        Label header = new Label(String.format("%-14s %8s %8s", "", "White", "Black"));
        header.setStyle("-fx-font-family: monospace;");
        contentBox.getChildren().add(header);
        for (MoveClassification c : MoveClassification.values()) {
            Label row = new Label(String.format("%-14s %8d %8d", c.label(),
                    analysis.getMoveStatistics().getCount(true, c),
                    analysis.getMoveStatistics().getCount(false, c)));
            row.setStyle("-fx-font-family: monospace;");
            contentBox.getChildren().add(row);
        }
    }

    private void addGamePhases() {
        addHeading("GAME PHASES");
        boolean any = false;
        for (PhaseStats ps : analysis.getPhaseStats()) {
            if (!ps.isPresent()) continue;
            any = true;
            contentBox.getChildren().add(new Label(String.format("%s   White: %.1f%%   Black: %.1f%%",
                    ps.getPhase(), ps.getWhiteAccuracy(), ps.getBlackAccuracy())));
        }
        if (!any) {
            contentBox.getChildren().add(new Label("No distinct game phases were detected."));
        }
    }

    private void addCriticalMoments() {
        addHeading("CRITICAL MOMENTS");
        if (analysis.getCriticalMoments().isEmpty()) {
            contentBox.getChildren().add(new Label("No critical moments detected."));
            return;
        }
        for (CriticalMoment cm : analysis.getCriticalMoments()) {
            VBox box = new VBox(2);
            box.setPadding(new Insets(6, 0, 6, 10));
            box.getStyleClass().add("critical-moment-" + cm.getClassification().name().toLowerCase());

            Label title = new Label(cm.getMoveLabel() + "   -   " + cm.getClassification().label());
            title.setStyle("-fx-font-weight: bold;");
            box.getChildren().add(title);

            if (cm.getEvalBefore() != null && cm.getEvalAfter() != null) {
                box.getChildren().add(new Label("Evaluation: " + cm.getEvalBefore().format() + "  ->  " + cm.getEvalAfter().format()));
            }
            if (cm.getBestMoveSan() != null && !cm.getBestMoveSan().isBlank()) {
                box.getChildren().add(new Label("Best move: " + cm.getBestMoveSan()));
            }
            if (cm.getPrincipalVariationSan() != null && !cm.getPrincipalVariationSan().isBlank()) {
                Label pv = new Label("Line: " + cm.getPrincipalVariationSan());
                pv.setWrapText(true);
                box.getChildren().add(pv);
            }
            if (cm.getReasonCategory() != null) {
                box.getChildren().add(new Label("Reason: " + cm.getReasonCategory()));
            }
            if (cm.getComment() != null && !cm.getComment().isBlank()) {
                Label comment = new Label(cm.getComment());
                comment.setWrapText(true);
                box.getChildren().add(comment);
            }
            contentBox.getChildren().add(box);
        }
    }

    private void addBulletSection(String heading, java.util.List<String> items, boolean numbered) {
        addHeading(heading);
        if (items.isEmpty()) {
            contentBox.getChildren().add(new Label("-"));
            return;
        }
        int i = 1;
        for (String item : items) {
            String prefix = numbered ? (i++) + ". " : "\u2022 ";
            Label label = new Label(prefix + item);
            label.setWrapText(true);
            contentBox.getChildren().add(label);
        }
    }

    private void addFinalAssessment(FinalReport report) {
        addHeading("FINAL ASSESSMENT");
        Label assessment = new Label(report.getFinalAssessment());
        assessment.setWrapText(true);
        contentBox.getChildren().add(assessment);
    }

    private void addHeading(String text) {
        Label heading = new Label(text);
        heading.getStyleClass().add("report-heading");
        contentBox.getChildren().add(new Separator());
        contentBox.getChildren().add(heading);
    }

    @FXML
    private void onExportTxt() {
        export("txt");
    }

    @FXML
    private void onExportJson() {
        export("json");
    }

    private void export(String type) {
        if (analysis == null) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Report");
        String defaultName = (analysis.getId() != null ? analysis.getId() : "report") + "." + type;
        chooser.setInitialFileName(defaultName);
        File file = chooser.showSaveDialog(contentBox.getScene().getWindow());
        if (file == null) return;

        try {
            if ("txt".equals(type)) {
                exportService.exportTxt(analysis, file);
            } else {
                exportService.exportJson(analysis, file);
            }
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Could not export report: " + e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void onReviewBoard() {
        if (analysis == null) return;
        GameAnalysis shown = analysis;
        AnalysisBoardView view = new AnalysisBoardView(shown, () -> {
            ReportController report = Main.loadScreen("/fxml/Report.fxml");
            report.showReport(shown);
        });
        Main.showView(view, 1240, 820);
    }

    @FXML
    private void onNewAnalysis() {
        Main.loadScreen("/fxml/Main.fxml");
    }
}

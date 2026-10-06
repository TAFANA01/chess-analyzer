package com.chessanalyzer.controller;

import com.chessanalyzer.Main;
import com.chessanalyzer.model.AnalysisSettings;
import com.chessanalyzer.model.ChessGame;
import com.chessanalyzer.model.GameAnalysis;
import com.chessanalyzer.repository.AnalysisRepository;
import com.chessanalyzer.service.AnalysisService;
import com.chessanalyzer.service.ReportService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AnalyzeController {

    private static final Logger LOG = Logger.getLogger(AnalyzeController.class.getName());

    @FXML private Label gameInfoLabel;
    @FXML private Label currentMoveLabel;
    @FXML private Label depthLabel;
    @FXML private Label moveCountLabel;
    @FXML private ProgressBar progressBar;
    @FXML private Button cancelButton;

    private final AnalysisService analysisService = new AnalysisService();
    private final ReportService reportService = new ReportService();
    private final AnalysisRepository repository = new AnalysisRepository();

    private final AtomicBoolean cancelFlag = new AtomicBoolean(false);

    public void startAnalysis(ChessGame game, AnalysisSettings settings) {
        gameInfoLabel.setText(game.getWhite() + " vs " + game.getBlack() + "  (" + game.getMoveCount() + " plies)");
        depthLabel.setText(String.valueOf(settings.getDepth()));
        progressBar.setProgress(0);
        cancelFlag.set(false);

        Task<GameAnalysis> task = new Task<>() {
            @Override
            protected GameAnalysis call() throws Exception {
                return analysisService.analyzeGame(game, settings, (current, total, moveLabel, depth) -> {
                    updateProgress(current, total);
                    Platform.runLater(() -> {
                        moveCountLabel.setText(current + " / " + total);
                        currentMoveLabel.setText(moveLabel);
                    });
                }, cancelFlag);
            }
        };

        task.setOnSucceeded(e -> onAnalysisFinished(task.getValue()));
        task.setOnFailed(e -> onAnalysisFailed(task.getException()));
        progressBar.progressProperty().bind(task.progressProperty());

        Thread analysisThread = new Thread(task, "analysis-thread");
        analysisThread.setDaemon(true);
        analysisThread.start();
    }

    @FXML
    private void onCancel() {
        cancelFlag.set(true);
        currentMoveLabel.setText("Cancelling...");
        cancelButton.setDisable(true);
    }

    private void onAnalysisFinished(GameAnalysis analysis) {
        reportService.generateReport(analysis);
        String saveError = null;
        try {
            repository.save(analysis);
        } catch (IOException e) {
            // Non-fatal for the report, but the user must know the analysis was NOT saved.
            LOG.log(Level.SEVERE, "Failed to save analysis", e);
            saveError = e.getMessage();
        }
        ReportController controller = Main.loadScreen("/fxml/Report.fxml");
        controller.showReport(analysis);
        if (saveError != null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Analysis not saved");
            alert.setHeaderText("The analysis is shown, but it could not be saved.");
            alert.setContentText(saveError);
            alert.show();
        }
    }

    private void onAnalysisFailed(Throwable ex) {
        LOG.log(Level.SEVERE, "Analysis failed", ex);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Analysis Failed");
        alert.setHeaderText("The analysis could not be completed.");
        alert.setContentText(ex != null ? ex.getMessage() : "Unknown error.");
        alert.showAndWait();
        Main.loadScreen("/fxml/Main.fxml");
    }
}

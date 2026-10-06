package com.chessanalyzer.controller;

import com.chessanalyzer.Main;
import com.chessanalyzer.model.AnalysisSettings;
import com.chessanalyzer.model.ChessGame;
import com.chessanalyzer.service.PgnParseResult;
import com.chessanalyzer.service.PgnService;
import com.chessanalyzer.service.PgnValidationException;
import com.chessanalyzer.util.FileUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;

public class MainController {

    @FXML private TextArea pgnTextArea;
    @FXML private ComboBox<AnalysisSettings.Mode> modeComboBox;
    @FXML private TextField stockfishPathField;
    @FXML private Label statusLabel;
    @FXML private Button analyzeButton;

    private final PgnService pgnService = new PgnService();

    @FXML
    public void initialize() {
        modeComboBox.getItems().addAll(AnalysisSettings.Mode.values());
        modeComboBox.getSelectionModel().select(AnalysisSettings.Mode.NORMAL);

        String detected = FileUtil.tryAutoDetectStockfish();
        if (detected != null) {
            stockfishPathField.setText(detected);
        }
    }

    @FXML
    private void onImportPgn() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Import PGN");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PGN files", "*.pgn"));
        File file = chooser.showOpenDialog(pgnTextArea.getScene().getWindow());
        if (file == null) return;

        try {
            pgnTextArea.setText(FileUtil.readTextFile(file));
            statusLabel.setText("Loaded: " + file.getName());
        } catch (IOException e) {
            statusLabel.setText("Could not read file: " + e.getMessage());
        }
    }

    @FXML
    private void onBrowseStockfish() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Stockfish executable");
        File initialDir = initialStockfishDirectory();
        if (initialDir != null) {
            chooser.setInitialDirectory(initialDir);
        }
        File file = chooser.showOpenDialog(stockfishPathField.getScene().getWindow());

        if (file != null) {
            stockfishPathField.setText(file.getAbsolutePath());
        }
    }

    /** Start the file dialog next to the currently configured executable, else the user's home. */
    private File initialStockfishDirectory() {
        String current = stockfishPathField.getText();
        if (current != null && !current.isBlank()) {
            File parent = new File(current).getParentFile();
            if (parent != null && parent.isDirectory()) {
                return parent;
            }
        }
        File home = new File(System.getProperty("user.home", "."));
        return home.isDirectory() ? home : null;
    }

    @FXML
    private void onAnalyzeGame() {
        String rawPgn = pgnTextArea.getText();
        String stockfishPath = stockfishPathField.getText();

        if (!FileUtil.isValidExecutable(stockfishPath)) {
            statusLabel.setText("Please select a valid Stockfish executable before analyzing.");
            return;
        }

        ChessGame game;
        try {
            PgnParseResult parsed = pgnService.parseAll(rawPgn);
            if (!parsed.hasGames()) {
                statusLabel.setText(parsed.errors().get(0).describe());
                return;
            }
            game = parsed.games().get(0);
            int total = parsed.games().size() + parsed.errors().size();
            if (total > 1) {
                statusLabel.setText("The file contains " + total + " games; analyzing the first valid one ("
                        + game.getWhite() + " vs " + game.getBlack() + ").");
            }
        } catch (PgnValidationException e) {
            statusLabel.setText(e.getMessage());
            return;
        }

        AnalysisSettings settings = new AnalysisSettings();
        settings.setMode(modeComboBox.getValue());
        settings.setStockfishPath(stockfishPath);

        AnalyzeController controller = Main.loadScreen("/fxml/Analyze.fxml");
        controller.startAnalysis(game, settings);
    }
}

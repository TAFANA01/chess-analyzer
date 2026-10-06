package com.chessanalyzer.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * The complete result of analyzing one game: everything needed to render
 * the final report and everything persisted to disk as JSON.
 */
public class GameAnalysis {

    /** Version of the persisted JSON layout; bump when the format changes and add a migration. */
    public static final int CURRENT_SCHEMA_VERSION = 2;

    private int schemaVersion = CURRENT_SCHEMA_VERSION;
    private String id; // e.g. "game_2026_09_25_001"
    private LocalDateTime analyzedAt = LocalDateTime.now();

    private ChessGame game;
    private AnalysisSettings settings;

    private final List<MoveAnalysis> moves = new ArrayList<>();
    private final List<CriticalMoment> criticalMoments = new ArrayList<>();
    private final List<PhaseStats> phaseStats = new ArrayList<>();

    private double whiteAccuracy;
    private double blackAccuracy;
    private MoveStatistics moveStatistics = new MoveStatistics();

    private FinalReport finalReport;

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(int schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDateTime getAnalyzedAt() {
        return analyzedAt;
    }

    public void setAnalyzedAt(LocalDateTime analyzedAt) {
        this.analyzedAt = analyzedAt;
    }

    public ChessGame getGame() {
        return game;
    }

    public void setGame(ChessGame game) {
        this.game = game;
    }

    public AnalysisSettings getSettings() {
        return settings;
    }

    public void setSettings(AnalysisSettings settings) {
        this.settings = settings;
    }

    public List<MoveAnalysis> getMoves() {
        return moves;
    }

    public List<CriticalMoment> getCriticalMoments() {
        return criticalMoments;
    }

    public List<PhaseStats> getPhaseStats() {
        return phaseStats;
    }

    public double getWhiteAccuracy() {
        return whiteAccuracy;
    }

    public void setWhiteAccuracy(double whiteAccuracy) {
        this.whiteAccuracy = whiteAccuracy;
    }

    public double getBlackAccuracy() {
        return blackAccuracy;
    }

    public void setBlackAccuracy(double blackAccuracy) {
        this.blackAccuracy = blackAccuracy;
    }

    public MoveStatistics getMoveStatistics() {
        return moveStatistics;
    }

    public void setMoveStatistics(MoveStatistics moveStatistics) {
        this.moveStatistics = moveStatistics;
    }

    public FinalReport getFinalReport() {
        return finalReport;
    }

    public void setFinalReport(FinalReport finalReport) {
        this.finalReport = finalReport;
    }
}

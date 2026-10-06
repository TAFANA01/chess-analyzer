package com.chessanalyzer.model;

public class PhaseStats {

    private GamePhase phase;
    private double whiteAccuracy;
    private double blackAccuracy;
    private int plyCount;
    private boolean present = true; // false = phase was not meaningfully present in this game

    public PhaseStats() {
    }

    public PhaseStats(GamePhase phase) {
        this.phase = phase;
    }

    public GamePhase getPhase() {
        return phase;
    }

    public void setPhase(GamePhase phase) {
        this.phase = phase;
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

    public int getPlyCount() {
        return plyCount;
    }

    public void setPlyCount(int plyCount) {
        this.plyCount = plyCount;
    }

    public boolean isPresent() {
        return present;
    }

    public void setPresent(boolean present) {
        this.present = present;
    }
}

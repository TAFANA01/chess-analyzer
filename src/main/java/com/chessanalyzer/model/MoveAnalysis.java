package com.chessanalyzer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Full analysis result for a single played move (one ply).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MoveAnalysis {

    private int moveNumber;          // full-move number (1, 1, 2, 2, 3, ...)
    private boolean whiteToMove;     // true if this ply was played by White
    private String san;              // e.g. "Qe7"
    private String uci;              // e.g. "e7e6"
    private String fenBefore;
    private String fenAfter;

    private Evaluation evalBefore;   // best-case evaluation of the position before the move
    private Evaluation evalAfter;    // evaluation after the move was actually played
    private String bestMoveSan;      // engine's preferred move in that position
    private String bestMoveUci;
    private String principalVariationSan; // short PV starting from bestMove

    private double evalLossCentipawns; // how much worse the position became vs. the engine's best line
    private MoveClassification classification;
    private String comment;          // human-readable explanation (filled for notable moves)
    private String reasonCategory;   // e.g. "Tactical mistake", "King safety", ...

    private int engineDepth;        // depth the engine reached for the position before this move
    private int engineDepthAfter;   // depth the engine reached for the position after this move (0 = not recorded)

    private GamePhase phase;
    private boolean criticalMoment;

    public MoveAnalysis() {
    }

    // --- Getters / setters ---

    public int getMoveNumber() {
        return moveNumber;
    }

    public void setMoveNumber(int moveNumber) {
        this.moveNumber = moveNumber;
    }

    public boolean isWhiteToMove() {
        return whiteToMove;
    }

    public void setWhiteToMove(boolean whiteToMove) {
        this.whiteToMove = whiteToMove;
    }

    public String getSan() {
        return san;
    }

    public void setSan(String san) {
        this.san = san;
    }

    public String getUci() {
        return uci;
    }

    public void setUci(String uci) {
        this.uci = uci;
    }

    public String getFenBefore() {
        return fenBefore;
    }

    public void setFenBefore(String fenBefore) {
        this.fenBefore = fenBefore;
    }

    public String getFenAfter() {
        return fenAfter;
    }

    public void setFenAfter(String fenAfter) {
        this.fenAfter = fenAfter;
    }

    public Evaluation getEvalBefore() {
        return evalBefore;
    }

    public void setEvalBefore(Evaluation evalBefore) {
        this.evalBefore = evalBefore;
    }

    public Evaluation getEvalAfter() {
        return evalAfter;
    }

    public void setEvalAfter(Evaluation evalAfter) {
        this.evalAfter = evalAfter;
    }

    public String getBestMoveSan() {
        return bestMoveSan;
    }

    public void setBestMoveSan(String bestMoveSan) {
        this.bestMoveSan = bestMoveSan;
    }

    public String getBestMoveUci() {
        return bestMoveUci;
    }

    public void setBestMoveUci(String bestMoveUci) {
        this.bestMoveUci = bestMoveUci;
    }

    public String getPrincipalVariationSan() {
        return principalVariationSan;
    }

    public void setPrincipalVariationSan(String principalVariationSan) {
        this.principalVariationSan = principalVariationSan;
    }

    public double getEvalLossCentipawns() {
        return evalLossCentipawns;
    }

    public void setEvalLossCentipawns(double evalLossCentipawns) {
        this.evalLossCentipawns = evalLossCentipawns;
    }

    public MoveClassification getClassification() {
        return classification;
    }

    public void setClassification(MoveClassification classification) {
        this.classification = classification;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getReasonCategory() {
        return reasonCategory;
    }

    public void setReasonCategory(String reasonCategory) {
        this.reasonCategory = reasonCategory;
    }

    public GamePhase getPhase() {
        return phase;
    }

    public void setPhase(GamePhase phase) {
        this.phase = phase;
    }

    public boolean isCriticalMoment() {
        return criticalMoment;
    }

    public void setCriticalMoment(boolean criticalMoment) {
        this.criticalMoment = criticalMoment;
    }

    /** e.g. "18...Qe7" or "24.Rxd5" */
    public String moveLabel() {
        return whiteToMove
                ? moveNumber + "." + san
                : moveNumber + "..." + san;
    }

    public int getEngineDepth() {
        return engineDepth;
    }

    public void setEngineDepth(int engineDepth) {
        this.engineDepth = engineDepth;
    }

    public int getEngineDepthAfter() {
        return engineDepthAfter;
    }

    public void setEngineDepthAfter(int engineDepthAfter) {
        this.engineDepthAfter = engineDepthAfter;
    }
}

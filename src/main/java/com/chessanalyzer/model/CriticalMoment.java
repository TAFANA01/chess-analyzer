package com.chessanalyzer.model;

/**
 * A single notable turning point selected for the report (mistake, blunder,
 * or missed opportunity worth showing to the user).
 */
public class CriticalMoment {

    private String moveLabel;        // "24.Rxd5"
    private String playedSan;
    private MoveClassification classification;
    private Evaluation evalBefore;
    private Evaluation evalAfter;
    private String bestMoveSan;
    private String principalVariationSan;
    private String comment;
    private String reasonCategory;

    public CriticalMoment() {
    }

    public static CriticalMoment from(MoveAnalysis m) {
        CriticalMoment c = new CriticalMoment();
        c.moveLabel = m.moveLabel();
        c.playedSan = m.getSan();
        c.classification = m.getClassification();
        c.evalBefore = m.getEvalBefore();
        c.evalAfter = m.getEvalAfter();
        c.bestMoveSan = m.getBestMoveSan();
        c.principalVariationSan = m.getPrincipalVariationSan();
        c.comment = m.getComment();
        c.reasonCategory = m.getReasonCategory();
        return c;
    }

    public String getMoveLabel() {
        return moveLabel;
    }

    public void setMoveLabel(String moveLabel) {
        this.moveLabel = moveLabel;
    }

    public String getPlayedSan() {
        return playedSan;
    }

    public void setPlayedSan(String playedSan) {
        this.playedSan = playedSan;
    }

    public MoveClassification getClassification() {
        return classification;
    }

    public void setClassification(MoveClassification classification) {
        this.classification = classification;
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

    public String getPrincipalVariationSan() {
        return principalVariationSan;
    }

    public void setPrincipalVariationSan(String principalVariationSan) {
        this.principalVariationSan = principalVariationSan;
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
}

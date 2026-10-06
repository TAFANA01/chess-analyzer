package com.chessanalyzer.model;

import java.util.ArrayList;
import java.util.List;

public class FinalReport {

    private String openingEvaluationSummary;
    private String middlegameEvaluationSummary;
    private String criticalMomentSummary; // e.g. "Move 24"
    private String postCriticalMomentSummary;
    private String finalEvaluationSummary;

    private final List<String> strongPoints = new ArrayList<>();
    private final List<String> weakPoints = new ArrayList<>();
    private final List<String> recommendations = new ArrayList<>();

    private String finalAssessment;

    public String getOpeningEvaluationSummary() {
        return openingEvaluationSummary;
    }

    public void setOpeningEvaluationSummary(String openingEvaluationSummary) {
        this.openingEvaluationSummary = openingEvaluationSummary;
    }

    public String getMiddlegameEvaluationSummary() {
        return middlegameEvaluationSummary;
    }

    public void setMiddlegameEvaluationSummary(String middlegameEvaluationSummary) {
        this.middlegameEvaluationSummary = middlegameEvaluationSummary;
    }

    public String getCriticalMomentSummary() {
        return criticalMomentSummary;
    }

    public void setCriticalMomentSummary(String criticalMomentSummary) {
        this.criticalMomentSummary = criticalMomentSummary;
    }

    public String getPostCriticalMomentSummary() {
        return postCriticalMomentSummary;
    }

    public void setPostCriticalMomentSummary(String postCriticalMomentSummary) {
        this.postCriticalMomentSummary = postCriticalMomentSummary;
    }

    public String getFinalEvaluationSummary() {
        return finalEvaluationSummary;
    }

    public void setFinalEvaluationSummary(String finalEvaluationSummary) {
        this.finalEvaluationSummary = finalEvaluationSummary;
    }

    public List<String> getStrongPoints() {
        return strongPoints;
    }

    public List<String> getWeakPoints() {
        return weakPoints;
    }

    public List<String> getRecommendations() {
        return recommendations;
    }

    public String getFinalAssessment() {
        return finalAssessment;
    }

    public void setFinalAssessment(String finalAssessment) {
        this.finalAssessment = finalAssessment;
    }
}

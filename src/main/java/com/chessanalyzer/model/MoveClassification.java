package com.chessanalyzer.model;

/**
 * Quality classification assigned to a single played move.
 * Order matters: ordinal() is used for display ordering in statistics tables
 * and as a "badness" ranking (higher ordinal = worse move, BRILLIANT being the best).
 */
public enum MoveClassification {
    BRILLIANT("Brilliant"),
    BEST("Best"),
    EXCELLENT("Excellent"),
    GOOD("Good"),
    INACCURACY("Inaccuracy"),
    MISTAKE("Mistake"),
    BLUNDER("Blunder");

    private final String label;

    MoveClassification(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}

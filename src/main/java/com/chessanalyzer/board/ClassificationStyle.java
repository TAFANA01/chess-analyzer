package com.chessanalyzer.board;

import com.chessanalyzer.model.MoveClassification;

/**
 * Visual identity of each move classification: the annotation symbol used next to a move
 * (chess notation style), a headline icon, a CSS class and a colour for canvas drawing.
 * The colours are mirrored in {@code board.css}.
 */
public enum ClassificationStyle {
    BRILLIANT(MoveClassification.BRILLIANT, "!!", "\u2605", "Brilliant", "cls-brilliant", "#1baca6"),
    BEST(MoveClassification.BEST, "!", "\u2605", "Best", "cls-best", "#81b64c"),
    EXCELLENT(MoveClassification.EXCELLENT, "", "\u2713", "Excellent", "cls-excellent", "#95b776"),
    GOOD(MoveClassification.GOOD, "", "\u2713", "Good", "cls-good", "#a0a8b5"),
    INACCURACY(MoveClassification.INACCURACY, "?!", "\u26A0", "Inaccuracy", "cls-inaccuracy", "#f7c631"),
    MISTAKE(MoveClassification.MISTAKE, "?", "\u2716", "Mistake", "cls-mistake", "#e58f2a"),
    BLUNDER(MoveClassification.BLUNDER, "??", "\u2716\u2716", "Blunder", "cls-blunder", "#ca3431");

    private final MoveClassification classification;
    private final String annotation;
    private final String icon;
    private final String label;
    private final String cssClass;
    private final String color;

    ClassificationStyle(MoveClassification classification, String annotation, String icon,
                        String label, String cssClass, String color) {
        this.classification = classification;
        this.annotation = annotation;
        this.icon = icon;
        this.label = label;
        this.cssClass = cssClass;
        this.color = color;
    }

    public static ClassificationStyle of(MoveClassification c) {
        if (c == null) return GOOD;
        for (ClassificationStyle s : values()) {
            if (s.classification == c) return s;
        }
        return GOOD;
    }

    /** Notation suffix: "??", "?", "?!", "!!", "!" or empty. */
    public String annotation() {
        return annotation;
    }

    /** Icon shown in lists and headlines. */
    public String icon() {
        return icon;
    }

    public String label() {
        return label;
    }

    public String cssClass() {
        return cssClass;
    }

    public String color() {
        return color;
    }

    /** Inaccuracy, mistake or blunder: the moves that get a best-move comparison. */
    public boolean isProblem() {
        return this == INACCURACY || this == MISTAKE || this == BLUNDER;
    }
}

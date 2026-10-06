package com.chessanalyzer.board;

import com.chessanalyzer.model.Evaluation;
import com.chessanalyzer.service.AccuracyService;

/** Turns an {@link Evaluation} (White's perspective) into display text and a bar fraction. */
public final class EvaluationFormatter {

    private static final AccuracyService WIN_CURVE = new AccuracyService();

    private EvaluationFormatter() {
    }

    /** Compact text: "+1.80", "-2.10", "0.00", "+M3", "-M2", "1-0" (mate delivered) or "-" if unknown. */
    public static String compact(Evaluation e) {
        if (e == null) return "\u2013";
        if (e.isMate()) {
            if (e.getMateIn() == 0) return e.mateSign() > 0 ? "1-0" : "0-1";
            return (e.mateSign() > 0 ? "+M" : "-M") + Math.abs(e.getMateIn());
        }
        int cp = e.getCentipawns();
        if (cp == 0) return "0.00";
        return String.format(java.util.Locale.ROOT, "%+.2f", cp / 100.0);
    }

    /** Headline text: "MATE IN 3", "CHECKMATE", or the compact numeric value. */
    public static String headline(Evaluation e) {
        if (e == null) return "\u2013";
        if (e.isMate()) {
            if (e.getMateIn() == 0) return "CHECKMATE";
            return "MATE IN " + Math.abs(e.getMateIn());
        }
        return compact(e);
    }

    /** One-line explanation, e.g. "Black has a forced mate in 2" or "White is slightly better". */
    public static String describe(Evaluation e) {
        if (e == null) return "No evaluation available";
        if (e.isMate()) {
            String side = e.mateSign() > 0 ? "White" : "Black";
            if (e.getMateIn() == 0) return side + " delivered checkmate";
            return side + " has a forced mate in " + Math.abs(e.getMateIn());
        }
        int cp = e.getCentipawns();
        int abs = Math.abs(cp);
        if (abs < 20) return "The position is equal";
        String side = cp > 0 ? "White" : "Black";
        if (abs < 80) return side + " is slightly better";
        if (abs < 200) return side + " has a clear advantage";
        if (abs < 500) return side + " has a large advantage";
        return side + " is winning";
    }

    /** Share of the bar (0..1) that belongs to White. Mates fill the bar completely. */
    public static double whiteShare(Evaluation e) {
        if (e == null) return 0.5;
        if (e.isMate()) return e.mateSign() > 0 ? 1.0 : 0.0;
        return WIN_CURVE.winPercent(e.getCentipawns()) / 100.0;
    }
}

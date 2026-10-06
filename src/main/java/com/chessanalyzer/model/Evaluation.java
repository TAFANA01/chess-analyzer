package com.chessanalyzer.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A single engine evaluation, always normalized to White's perspective.
 * Either a centipawn score, or a forced mate in N (positive N = White mates,
 * negative N = Black mates).
 */
public class Evaluation {

    private final Integer centipawns; // null if mate
    private final Integer mateIn;     // null if not a forced mate; 0 = checkmate already on the board
    /** Only meaningful when {@code mateIn == 0}: true if White delivered the mate. Null otherwise. */
    private final Boolean whiteDeliveredMate;

    @JsonCreator
    private Evaluation(@JsonProperty("centipawns") Integer centipawns,
                        @JsonProperty("mateIn") Integer mateIn,
                        @JsonProperty("whiteDeliveredMate") Boolean whiteDeliveredMate) {
        this.centipawns = centipawns;
        this.mateIn = mateIn;
        this.whiteDeliveredMate = whiteDeliveredMate;
    }

    public static Evaluation ofCentipawns(int cp) {
        return new Evaluation(cp, null, null);
    }

    /** Forced mate in {@code mateIn} moves; positive = White mates, negative = Black mates. Must not be 0. */
    public static Evaluation ofMate(int mateIn) {
        if (mateIn == 0) {
            throw new IllegalArgumentException("Use ofCheckmate(...) for a position that is already mate");
        }
        return new Evaluation(null, mateIn, null);
    }

    /** The position on the board is already checkmate (the side to move has been mated). */
    public static Evaluation ofCheckmate(boolean whiteDeliveredMate) {
        return new Evaluation(null, 0, whiteDeliveredMate);
    }

    public boolean isMate() {
        return mateIn != null;
    }

    public Integer getCentipawns() {
        return centipawns;
    }

    public Integer getMateIn() {
        return mateIn;
    }

    public Boolean getWhiteDeliveredMate() {
        return whiteDeliveredMate;
    }

    /** +1 if the mate favours White, -1 if it favours Black. Only valid when {@link #isMate()}. */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public int mateSign() {
        if (mateIn == 0) {
            return Boolean.TRUE.equals(whiteDeliveredMate) ? 1 : -1;
        }
        return mateIn > 0 ? 1 : -1;
    }

    /**
     * A single comparable number for internal math (loss calculation, sorting, etc).
     * Mates are clamped to a large constant so they dominate comparisons without overflowing.
     */
    public double asComparable() {
        if (isMate()) {
            double sign = mateSign();
            // Closer mates are "more winning" than farther ones, but all mates dominate any cp score.
            return sign * (10_000 - Math.abs(mateIn));
        }
        return centipawns;
    }

    public String format() {
        if (isMate()) {
            if (mateIn == 0) return mateSign() > 0 ? "1-0" : "0-1";
            return "#" + mateIn;
        }
        double pawns = centipawns / 100.0;
        return (pawns >= 0 ? "+" : "") + String.format("%.2f", pawns);
    }

    @Override
    public String toString() {
        return format();
    }
}

package com.chessanalyzer.service;

import com.chessanalyzer.model.GamePhase;
import com.chessanalyzer.model.MoveAnalysis;
import com.chessanalyzer.model.MoveClassification;

/**
 * Generates short, human-readable comments and a conservative "reason
 * category" for notable moves (inaccuracies, mistakes, blunders).
 *
 * Reason detection is intentionally shallow and rule-based -- it only uses
 * signals directly supported by what was actually analyzed (evaluation
 * swing size, game phase, whether the move was a capture). When nothing
 * reliable can be inferred, it falls back to a neutral category rather
 * than guessing.
 */
public class CommentService {

    /** Only mistakes, blunders, and inaccuracies get a full comment + reason. */
    public void annotate(MoveAnalysis move) {
        MoveClassification c = move.getClassification();
        if (c == null) return;
        // Re-annotation must not leave stale text behind if the classification changed.
        move.setComment(null);
        move.setReasonCategory(null);

        if (c == MoveClassification.MISTAKE || c == MoveClassification.BLUNDER || c == MoveClassification.INACCURACY) {
            String reason = determineReasonCategory(move);
            move.setReasonCategory(reason);
            move.setComment(buildComment(move, reason));
        } else if (c == MoveClassification.BEST || c == MoveClassification.EXCELLENT) {
            // Keep these silent by default; the report shows only critical moments.
        }
    }

    private String determineReasonCategory(MoveAnalysis move) {
        MoveClassification c = move.getClassification();
        double loss = move.getEvalLossCentipawns();
        GamePhase phase = move.getPhase();
        boolean looksLikeCapture = move.getSan() != null && move.getSan().contains("x");

        // Large, sudden swings after a capture read most reliably as tactical/material issues.
        if ((c == MoveClassification.BLUNDER) && looksLikeCapture && loss >= 300) {
            return "Tactical mistake";
        }
        if (c == MoveClassification.BLUNDER && loss >= 400) {
            return "Material loss";
        }
        if (phase == GamePhase.OPENING) {
            return "Development";
        }
        if (phase == GamePhase.ENDGAME) {
            return "Endgame technique";
        }
        if (c == MoveClassification.INACCURACY) {
            return "Missed opportunity";
        }
        return "Calculation error";
    }

    private String buildComment(MoveAnalysis move, String reason) {
        MoveClassification c = move.getClassification();
        String base = switch (c) {
            case BLUNDER -> "This move loses a significant part of the position's value"
                    + (reason.equals("Material loss") || reason.equals("Tactical mistake")
                    ? ", allowing the opponent to win material or force a decisive sequence."
                    : ", handing the opponent a much stronger position.");
            case MISTAKE -> "This move gives up a meaningful part of the advantage "
                    + "and allows the opponent to improve their position.";
            case INACCURACY -> "This move is playable, but a stronger continuation was available.";
            default -> "";
        };
        return base;
    }
}

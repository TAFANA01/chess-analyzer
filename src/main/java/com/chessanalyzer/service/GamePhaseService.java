package com.chessanalyzer.service;

import com.chessanalyzer.model.GamePhase;
import com.chessanalyzer.model.MoveAnalysis;
import com.chessanalyzer.model.PhaseStats;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits a game into Opening / Middlegame / Endgame and computes per-phase
 * accuracy for both players.
 *
 * Heuristic (deliberately simple for V1):
 *  - Opening lasts through the first {@link #OPENING_MOVE_LIMIT} full moves,
 *    unless the endgame is reached first (rare, but possible in a blitz
 *    brawl with early piece trades).
 *  - Endgame begins at the first ply where combined non-pawn material
 *    (queens=9, rooks=5, minors=3, summed across BOTH sides) drops to or
 *    below {@link #ENDGAME_MATERIAL_THRESHOLD}.
 *  - Everything in between is the middlegame.
 *  - If the endgame threshold is never reached, there is no endgame phase
 *    (PhaseStats.present = false), matching the report requirement to omit
 *    a meaningless endgame section.
 */
public class GamePhaseService {

    private static final int OPENING_MOVE_LIMIT = 10;
    private static final int ENDGAME_MATERIAL_THRESHOLD = 26; // e.g. ~ two rooks + two minors combined

    public void assignPhases(List<MoveAnalysis> moves) {
        int endgameStartPly = detectEndgameStartPly(moves);

        for (int i = 0; i < moves.size(); i++) {
            MoveAnalysis m = moves.get(i);
            int ply = i + 1;
            GamePhase phase;
            if (ply >= endgameStartPly) {
                phase = GamePhase.ENDGAME;
            } else if (m.getMoveNumber() <= OPENING_MOVE_LIMIT) {
                phase = GamePhase.OPENING;
            } else {
                phase = GamePhase.MIDDLEGAME;
            }
            m.setPhase(phase);
        }
    }

    private int detectEndgameStartPly(List<MoveAnalysis> moves) {
        for (int i = 0; i < moves.size(); i++) {
            MoveAnalysis m = moves.get(i);
            if (m.getFenAfter() == null) continue;
            if (materialScore(m.getFenAfter()) <= ENDGAME_MATERIAL_THRESHOLD) {
                return i + 1;
            }
        }
        return Integer.MAX_VALUE; // endgame never reached
    }

    /** Combined non-pawn material for both sides, from the board part of a FEN. */
    private int materialScore(String fen) {
        String boardPart = fen.split(" ")[0];
        int total = 0;
        for (char c : boardPart.toCharArray()) {
            switch (Character.toLowerCase(c)) {
                case 'q' -> total += 9;
                case 'r' -> total += 5;
                case 'b', 'n' -> total += 3;
                default -> {
                }
            }
        }
        return total;
    }

    public List<PhaseStats> computePhaseStats(List<MoveAnalysis> moves, AccuracyService accuracyService) {
        List<PhaseStats> result = new ArrayList<>();
        for (GamePhase phase : GamePhase.values()) {
            List<MoveAnalysis> subset = moves.stream().filter(m -> m.getPhase() == phase).toList();
            PhaseStats stats = new PhaseStats(phase);
            if (subset.isEmpty()) {
                stats.setPresent(false);
            } else {
                AccuracyResult r = accuracyService.computeForGame(subset);
                stats.setWhiteAccuracy(r.whiteAccuracy());
                stats.setBlackAccuracy(r.blackAccuracy());
                stats.setPlyCount(subset.size());
            }
            result.add(stats);
        }
        return result;
    }
}

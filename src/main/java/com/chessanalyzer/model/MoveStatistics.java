package com.chessanalyzer.model;

import java.util.EnumMap;
import java.util.Map;

public class MoveStatistics {

    private final Map<MoveClassification, Integer> whiteCounts = new EnumMap<>(MoveClassification.class);
    private final Map<MoveClassification, Integer> blackCounts = new EnumMap<>(MoveClassification.class);

    public MoveStatistics() {
        for (MoveClassification c : MoveClassification.values()) {
            whiteCounts.put(c, 0);
            blackCounts.put(c, 0);
        }
    }

    public void record(boolean whiteMove, MoveClassification classification) {
        if (classification == null) {
            return; // an unclassified move (e.g. missing evaluation) is simply not counted
        }
        Map<MoveClassification, Integer> map = whiteMove ? whiteCounts : blackCounts;
        map.merge(classification, 1, Integer::sum);
    }

    public int getCount(boolean white, MoveClassification classification) {
        return (white ? whiteCounts : blackCounts).getOrDefault(classification, 0);
    }

    public Map<MoveClassification, Integer> getWhiteCounts() {
        return whiteCounts;
    }

    public Map<MoveClassification, Integer> getBlackCounts() {
        return blackCounts;
    }
}

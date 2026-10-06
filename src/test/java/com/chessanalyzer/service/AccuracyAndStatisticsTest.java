package com.chessanalyzer.service;

import com.chessanalyzer.model.Evaluation;
import com.chessanalyzer.model.GameAnalysis;
import com.chessanalyzer.model.MoveAnalysis;
import com.chessanalyzer.model.MoveClassification;
import com.chessanalyzer.support.TestData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AccuracyAndStatisticsTest {

    private final AccuracyService accuracy = new AccuracyService();

    @Test
    void winPercentIsMonotonicAndBounded() {
        double prev = -1;
        for (int cp = -2000; cp <= 2000; cp += 100) {
            double w = accuracy.winPercent(cp);
            assertTrue(w >= 0 && w <= 100);
            assertTrue(w >= prev);
            prev = w;
        }
        assertEquals(50, accuracy.winPercent(0), 0.001);
    }

    @Test
    void noWinPercentDropMeansNearPerfectMoveAccuracy() {
        assertTrue(accuracy.moveAccuracy(55, 55) > 99.5);
        assertTrue(accuracy.moveAccuracy(55, 60) > 99.5, "improving never lowers accuracy");
    }

    @Test
    void biggerDropMeansLowerAccuracy() {
        assertTrue(accuracy.moveAccuracy(50, 40) > accuracy.moveAccuracy(50, 20));
        assertTrue(accuracy.moveAccuracy(90, 10) < 10);
    }

    @Test
    void gameWithOnlyBestMovesIsNearPerfect() {
        List<MoveAnalysis> moves = List.of(
                TestData.move(0, "e4", "e2e4", true, Evaluation.ofCentipawns(30), Evaluation.ofCentipawns(30), "e2e4"),
                TestData.move(1, "e5", "e7e5", false, Evaluation.ofCentipawns(30), Evaluation.ofCentipawns(30), "e7e5"));
        AccuracyResult r = accuracy.computeForGame(moves);
        assertTrue(r.whiteAccuracy() > 99);
        assertTrue(r.blackAccuracy() > 99);
    }

    @Test
    void blunderLowersOnlyTheBlunderingSide() {
        List<MoveAnalysis> moves = List.of(
                TestData.move(0, "e4", "e2e4", true, Evaluation.ofCentipawns(30), Evaluation.ofCentipawns(30), "e2e4"),
                TestData.move(1, "f6", "f7f6", false, Evaluation.ofCentipawns(30), Evaluation.ofCentipawns(900), "e7e5"));
        AccuracyResult r = accuracy.computeForGame(moves);
        assertTrue(r.whiteAccuracy() > 99);
        assertTrue(r.blackAccuracy() < 20);
    }

    @Test
    void deliveringCheckmateDoesNotPenaliseTheWinner() {
        // Regression: a mating move used to be scored as a ~20 000cp loss.
        List<MoveAnalysis> moves = List.of(
                TestData.move(0, "Qe6#", "e1e6", true, Evaluation.ofMate(1), Evaluation.ofCheckmate(true), "e1e6"));
        assertTrue(accuracy.computeForGame(moves).whiteAccuracy() > 99);
    }

    @Test
    void emptyGameDefaultsToFullAccuracyWithoutCrashing() {
        AccuracyResult r = accuracy.computeForGame(List.of());
        assertEquals(100, r.whiteAccuracy());
        assertEquals(100, r.blackAccuracy());
    }

    @Test
    void finalizationProducesConsistentStatisticsAndCpl() {
        GameAnalysis a = TestData.sampleAnalysis("Alice", "Bob");
        // Ply 3 (Nf3): 25 -> -150 = 175cp loss for White => MISTAKE, best move was d4.
        MoveAnalysis nf3 = a.getMoves().get(2);
        assertEquals(175, nf3.getEvalLossCentipawns(), 0.001);
        assertEquals(MoveClassification.MISTAKE, nf3.getClassification());

        assertEquals(1, a.getMoveStatistics().getCount(true, MoveClassification.MISTAKE));
        assertEquals(1, a.getMoveStatistics().getCount(true, MoveClassification.BEST));
        assertEquals(2, a.getMoveStatistics().getCount(false, MoveClassification.BEST));
        int total = 0;
        for (MoveClassification c : MoveClassification.values()) {
            total += a.getMoveStatistics().getCount(true, c) + a.getMoveStatistics().getCount(false, c);
        }
        assertEquals(a.getMoves().size(), total, "every move counted exactly once");
        assertTrue(a.getWhiteAccuracy() < a.getBlackAccuracy());
        assertNotNull(nf3.getComment(), "mistakes get an explanation");
    }

    @Test
    void recalculationIsIdempotent() {
        GameAnalysis a = TestData.sampleAnalysis("A", "B");
        double w = a.getWhiteAccuracy();
        int mistakes = a.getMoveStatistics().getCount(true, MoveClassification.MISTAKE);
        new AnalysisRecalculator().recalculate(a);
        new AnalysisRecalculator().recalculate(a);
        assertEquals(w, a.getWhiteAccuracy(), 1e-9);
        assertEquals(mistakes, a.getMoveStatistics().getCount(true, MoveClassification.MISTAKE));
    }
}

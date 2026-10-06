package com.chessanalyzer.service;

import com.chessanalyzer.model.ClassificationThresholds;
import com.chessanalyzer.model.Evaluation;
import com.chessanalyzer.model.MoveClassification;
import org.junit.jupiter.api.Test;

import static com.chessanalyzer.model.MoveClassification.*;
import static org.junit.jupiter.api.Assertions.*;

class MoveClassificationServiceTest {

    private final MoveClassificationService service = new MoveClassificationService();

    /** White plays a non-engine move; evals are White-perspective centipawns. */
    private MoveClassification whitePlays(int before, int after) {
        return service.classify(Evaluation.ofCentipawns(before), Evaluation.ofCentipawns(after), true, "a2a3", "e2e4");
    }

    @Test
    void engineTopMoveIsBestEvenWithSearchNoise() {
        assertEquals(BEST, service.classify(Evaluation.ofCentipawns(50), Evaluation.ofCentipawns(10), true, "e2e4", "e2e4"));
    }

    @Test
    void nonTopMoveWithZeroLossIsNeverBest() {
        assertEquals(EXCELLENT, whitePlays(30, 40));
    }

    @Test
    void boundariesAreInclusive() {
        assertEquals(EXCELLENT, whitePlays(0, -20));
        assertEquals(GOOD, whitePlays(0, -21));
        assertEquals(GOOD, whitePlays(0, -60));
        assertEquals(INACCURACY, whitePlays(0, -61));
        assertEquals(INACCURACY, whitePlays(0, -120));
        assertEquals(MISTAKE, whitePlays(0, -121));
        assertEquals(MISTAKE, whitePlays(0, -250));
        assertEquals(BLUNDER, whitePlays(0, -251));
    }

    @Test
    void blackLossIsMeasuredFromBlacksPerspective() {
        // Evaluations are White-perspective: 0 -> +300 is a 300cp loss for Black.
        assertEquals(BLUNDER, service.classify(Evaluation.ofCentipawns(0), Evaluation.ofCentipawns(300), false, "a7a6", "e7e5"));
        assertEquals(EXCELLENT, service.classify(Evaluation.ofCentipawns(0), Evaluation.ofCentipawns(-300), false, "a7a6", "e7e5"));
    }

    @Test
    void decisiveAdvantageSoftensThresholds() {
        assertEquals(BLUNDER, whitePlays(0, -400));
        assertEquals(MISTAKE, whitePlays(900, 500)); // 400 loss scaled by 0.5 => 200
    }

    @Test
    void allowingMateIsABlunder() {
        assertEquals(BLUNDER, service.classify(Evaluation.ofCentipawns(100), Evaluation.ofMate(-3), true, "a2a3", "e2e4"));
    }

    @Test
    void missingMateIsPunished() {
        assertEquals(BLUNDER, service.classify(Evaluation.ofMate(2), Evaluation.ofCentipawns(150), true, "a2a3", "d1h5"));
    }

    @Test
    void brilliantRequiresSacrificeNearBestAndSoundPosition() {
        Evaluation before = Evaluation.ofCentipawns(50);
        Evaluation after = Evaluation.ofCentipawns(60);
        assertEquals(BRILLIANT, service.classify(before, after, true, "d1h5", "d1h5", 300));
        assertEquals(BEST, service.classify(before, after, true, "d1h5", "d1h5", 0), "no sacrifice => plain best");
        assertNotEquals(BRILLIANT, service.classify(before, Evaluation.ofCentipawns(-400), true, "d1h5", "e2e4", 300),
                "a sacrifice that loses is not brilliant");
        assertNotEquals(BRILLIANT, service.classify(Evaluation.ofCentipawns(900), Evaluation.ofCentipawns(900), true, "d1h5", "d1h5", 300),
                "already totally winning");
        assertNotEquals(BRILLIANT, service.classify(Evaluation.ofCentipawns(-500), Evaluation.ofCentipawns(-480), true, "d1h5", "d1h5", 300),
                "already clearly lost");
    }

    @Test
    void customThresholdsAreHonoured() {
        MoveClassificationService strict = new MoveClassificationService(
                new ClassificationThresholds(5, 10, 20, 40, 600, 0.5, 200, 20));
        assertEquals(INACCURACY, strict.classify(Evaluation.ofCentipawns(0), Evaluation.ofCentipawns(-15), true, "a2a3", "e2e4"));
        assertEquals(MISTAKE, strict.classify(Evaluation.ofCentipawns(0), Evaluation.ofCentipawns(-30), true, "a2a3", "e2e4"));
        assertEquals(BLUNDER, strict.classify(Evaluation.ofCentipawns(0), Evaluation.ofCentipawns(-41), true, "a2a3", "e2e4"));
    }

    @Test
    void invalidThresholdsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ClassificationThresholds(50, 20, 120, 250, 600, 0.5, 200, 20));
        assertThrows(IllegalArgumentException.class, () -> new ClassificationThresholds(20, 60, 120, 250, 600, 0, 200, 20));
    }
}

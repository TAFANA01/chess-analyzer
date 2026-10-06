package com.chessanalyzer.model;

import com.chessanalyzer.util.EvaluationUtil;
import com.chessanalyzer.util.JsonUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EvaluationTest {

    @Test
    void centipawnFormatting() {
        assertEquals("+0.35", Evaluation.ofCentipawns(35).format());
        assertEquals("-2.10", Evaluation.ofCentipawns(-210).format());
        assertEquals("+0.00", Evaluation.ofCentipawns(0).format());
    }

    @Test
    void mateFormattingAndSign() {
        assertEquals("#3", Evaluation.ofMate(3).format());
        assertEquals("#-2", Evaluation.ofMate(-2).format());
        assertEquals(1, Evaluation.ofMate(3).mateSign());
        assertEquals(-1, Evaluation.ofMate(-3).mateSign());
    }

    @Test
    void mateInZeroIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Evaluation.ofMate(0));
    }

    @Test
    void deliveredCheckmateHasCorrectSide() {
        Evaluation blackMated = Evaluation.ofCheckmate(true);
        Evaluation whiteMated = Evaluation.ofCheckmate(false);
        assertTrue(blackMated.asComparable() > 9000, "White delivered mate => hugely positive for White");
        assertTrue(whiteMated.asComparable() < -9000);
        assertEquals("1-0", blackMated.format());
        assertEquals("0-1", whiteMated.format());
    }

    @Test
    void fasterMateIsBetterThanSlowerMate() {
        assertTrue(Evaluation.ofMate(2).asComparable() > Evaluation.ofMate(5).asComparable());
        assertTrue(Evaluation.ofMate(-2).asComparable() < Evaluation.ofMate(-5).asComparable());
    }

    @Test
    void anyMateBeatsAnyCentipawnScore() {
        assertTrue(Evaluation.ofMate(40).asComparable() > Evaluation.ofCentipawns(3000).asComparable());
    }

    @Test
    void jsonRoundTripKeepsAllVariants() throws Exception {
        for (Evaluation e : new Evaluation[]{Evaluation.ofCentipawns(-87), Evaluation.ofMate(4),
                Evaluation.ofMate(-1), Evaluation.ofCheckmate(true), Evaluation.ofCheckmate(false)}) {
            Evaluation back = JsonUtil.mapper().readValue(JsonUtil.toJson(e), Evaluation.class);
            assertEquals(e.format(), back.format());
            assertEquals(e.asComparable(), back.asComparable());
        }
    }

    @Test
    void lossIsClampedSoMatesDoNotExplode() {
        // White is +3.0 and then allows mate: loss is bounded (300 -> -1000 clamp = 1300), not ~10 000.
        double loss = EvaluationUtil.lossForMover(Evaluation.ofCentipawns(300), Evaluation.ofMate(-2), true);
        assertEquals(1300, loss, 0.001);
    }

    @Test
    void deliveringMateCostsNothing() {
        assertEquals(0, EvaluationUtil.lossForMover(Evaluation.ofMate(1), Evaluation.ofCheckmate(true), true), 0.001);
        assertEquals(0, EvaluationUtil.lossForMover(Evaluation.ofMate(-1), Evaluation.ofCheckmate(false), false), 0.001);
    }

    @Test
    void lossFromBlackPerspective() {
        assertEquals(200, EvaluationUtil.lossForMover(Evaluation.ofCentipawns(-100), Evaluation.ofCentipawns(100), false), 0.001);
    }

    @Test
    void improvingTheEvaluationIsNotNegativeLoss() {
        assertEquals(0, EvaluationUtil.lossForMover(Evaluation.ofCentipawns(0), Evaluation.ofCentipawns(80), true), 0.001);
    }
}

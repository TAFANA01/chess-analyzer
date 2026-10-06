package com.chessanalyzer.board;

import com.chessanalyzer.model.Evaluation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EvaluationFormatterTest {

    @Test
    void compactFormats() {
        assertEquals("+1.80", EvaluationFormatter.compact(Evaluation.ofCentipawns(180)));
        assertEquals("-2.10", EvaluationFormatter.compact(Evaluation.ofCentipawns(-210)));
        assertEquals("0.00", EvaluationFormatter.compact(Evaluation.ofCentipawns(0)));
        assertEquals("+M3", EvaluationFormatter.compact(Evaluation.ofMate(3)));
        assertEquals("-M2", EvaluationFormatter.compact(Evaluation.ofMate(-2)));
        assertEquals("1-0", EvaluationFormatter.compact(Evaluation.ofCheckmate(true)));
        assertEquals("0-1", EvaluationFormatter.compact(Evaluation.ofCheckmate(false)));
        assertEquals("\u2013", EvaluationFormatter.compact(null));
    }

    @Test
    void headlinesSayMateInsteadOfANumber() {
        assertEquals("MATE IN 3", EvaluationFormatter.headline(Evaluation.ofMate(3)));
        assertEquals("MATE IN 2", EvaluationFormatter.headline(Evaluation.ofMate(-2)));
        assertEquals("CHECKMATE", EvaluationFormatter.headline(Evaluation.ofCheckmate(true)));
        assertEquals("+0.40", EvaluationFormatter.headline(Evaluation.ofCentipawns(40)));
    }

    @Test
    void descriptionsNameTheCorrectSide() {
        assertTrue(EvaluationFormatter.describe(Evaluation.ofMate(3)).contains("White"));
        assertTrue(EvaluationFormatter.describe(Evaluation.ofMate(-3)).contains("Black"));
        assertTrue(EvaluationFormatter.describe(Evaluation.ofCentipawns(-300)).startsWith("Black"));
        assertTrue(EvaluationFormatter.describe(Evaluation.ofCentipawns(5)).contains("equal"));
        assertTrue(EvaluationFormatter.describe(Evaluation.ofCheckmate(false)).contains("Black"));
    }

    @Test
    void barShareIsMonotonicBoundedAndMatesFillTheBar() {
        double prev = -1;
        for (int cp = -1500; cp <= 1500; cp += 100) {
            double s = EvaluationFormatter.whiteShare(Evaluation.ofCentipawns(cp));
            assertTrue(s > 0 && s < 1);
            assertTrue(s >= prev);
            prev = s;
        }
        assertEquals(0.5, EvaluationFormatter.whiteShare(Evaluation.ofCentipawns(0)), 1e-9);
        assertEquals(1.0, EvaluationFormatter.whiteShare(Evaluation.ofMate(4)));
        assertEquals(0.0, EvaluationFormatter.whiteShare(Evaluation.ofMate(-4)));
        assertEquals(1.0, EvaluationFormatter.whiteShare(Evaluation.ofCheckmate(true)));
        assertEquals(0.0, EvaluationFormatter.whiteShare(Evaluation.ofCheckmate(false)));
        assertEquals(0.5, EvaluationFormatter.whiteShare(null));
    }

    @Test
    void classificationStylesCoverEveryClassification() {
        for (com.chessanalyzer.model.MoveClassification c : com.chessanalyzer.model.MoveClassification.values()) {
            ClassificationStyle s = ClassificationStyle.of(c);
            assertEquals(c.label(), s.label());
            assertTrue(s.cssClass().startsWith("cls-"));
            assertTrue(s.color().matches("#[0-9a-f]{6}"));
        }
        assertTrue(ClassificationStyle.of(com.chessanalyzer.model.MoveClassification.BLUNDER).isProblem());
        assertFalse(ClassificationStyle.of(com.chessanalyzer.model.MoveClassification.GOOD).isProblem());
        assertEquals("??", ClassificationStyle.of(com.chessanalyzer.model.MoveClassification.BLUNDER).annotation());
    }
}

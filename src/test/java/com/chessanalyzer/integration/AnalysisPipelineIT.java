package com.chessanalyzer.integration;

import com.chessanalyzer.model.*;
import com.chessanalyzer.repository.AnalysisRepository;
import com.chessanalyzer.service.AnalysisService;
import com.chessanalyzer.service.EngineException;
import com.chessanalyzer.service.PgnService;
import com.chessanalyzer.service.StockfishService;
import com.chessanalyzer.util.FileUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * End-to-end tests against a real Stockfish. They are skipped (not failed) when no
 * executable can be found; set -Dstockfish.path=/path/to/stockfish to force one.
 */
class AnalysisPipelineIT {

    private static String stockfish;

    // Fool's mate: 1. f3 e5 2. g4 Qh4#
    private static final String FOOLS_MATE = "[White \"Fool\"]\n[Black \"Mater\"]\n[Result \"0-1\"]\n\n1. f3 e5 2. g4 Qh4# 0-1";

    @BeforeAll
    static void locateEngine() {
        String configured = System.getProperty("stockfish.path");
        stockfish = FileUtil.isValidExecutable(configured) ? configured : FileUtil.tryAutoDetectStockfish();
        assumeTrue(stockfish != null, "Stockfish not available - integration tests skipped");
    }

    private static AnalysisSettings settings() {
        AnalysisSettings s = new AnalysisSettings(AnalysisSettings.Mode.QUICK, stockfish);
        s.setEngineThreads(1);
        s.setEngineHashMb(32);
        return s;
    }

    @Test
    void pgnToAnalysisFindsTheBlundersAndTheMate() throws Exception {
        ChessGame game = new PgnService().parse(FOOLS_MATE);
        AtomicInteger progress = new AtomicInteger();
        GameAnalysis a = new AnalysisService().analyzeGame(game, settings(),
                (cur, total, label, depth) -> progress.set(cur), null);

        assertEquals(4, a.getMoves().size());
        assertEquals(4, progress.get());
        for (MoveAnalysis m : a.getMoves()) {
            assertNotNull(m.getEvalBefore());
            assertNotNull(m.getEvalAfter());
            assertNotNull(m.getUci());
            assertNotNull(m.getClassification());
            assertTrue(m.getEngineDepth() > 0, "engine depth must be recorded");
        }
        MoveAnalysis g4 = a.getMoves().get(2);
        assertEquals("g4", g4.getSan());
        assertTrue(g4.getClassification() == MoveClassification.BLUNDER, "g4 allows mate in 1: " + g4.getClassification());

        MoveAnalysis mate = a.getMoves().get(3);
        assertEquals("Qh4#", mate.getSan());
        assertEquals(0, mate.getEvalLossCentipawns(), 0.001);
        assertTrue(mate.getEvalAfter().isMate());
        assertEquals(-1, mate.getEvalAfter().mateSign(), "Black delivered the mate");
        assertTrue(a.getBlackAccuracy() > a.getWhiteAccuracy());
    }

    @Test
    void analysisSurvivesSaveAndReload(@TempDir Path tmp) throws Exception {
        GameAnalysis a = new AnalysisService().analyzeGame(new PgnService().parse(FOOLS_MATE), settings(), null, null);
        AnalysisRepository repo = new AnalysisRepository(tmp);
        repo.save(a);

        GameAnalysis back = new AnalysisRepository(tmp).loadById(a.getId());
        assertEquals(a.getMoves().size(), back.getMoves().size());
        assertEquals(a.getWhiteAccuracy(), back.getWhiteAccuracy(), 1e-9);
        for (int i = 0; i < a.getMoves().size(); i++) {
            MoveAnalysis x = a.getMoves().get(i);
            MoveAnalysis y = back.getMoves().get(i);
            assertEquals(x.getSan(), y.getSan());
            assertEquals(x.getFenAfter(), y.getFenAfter());
            assertEquals(x.getClassification(), y.getClassification());
            assertEquals(x.getEvalAfter().format(), y.getEvalAfter().format());
            assertEquals(x.getBestMoveSan(), y.getBestMoveSan());
            boolean terminal = x.getEvalAfter().isMate() && x.getEvalAfter().getMateIn() == 0; // checkmate: nothing to search, depth 0
            assertEquals(!terminal, x.getEngineDepthAfter() > 0, "depth of the 'after' search is recorded (move " + x.getSan() + ")");
            assertEquals(x.getEngineDepthAfter(), y.getEngineDepthAfter(), "and persisted");
            assertEquals(x.getMoveNumber(), y.getMoveNumber());
        }
    }

    @Test
    void eachPositionIsSearchedOnce() throws Exception {
        // 4 plies => 5 distinct positions. With the position cache the engine reports depth for each once;
        // we assert indirectly that fenAfter(n) == fenBefore(n+1) and that evals are shared.
        GameAnalysis a = new AnalysisService().analyzeGame(new PgnService().parse(FOOLS_MATE), settings(), null, null);
        for (int i = 0; i + 1 < a.getMoves().size(); i++) {
            assertEquals(a.getMoves().get(i).getFenAfter(), a.getMoves().get(i + 1).getFenBefore());
            assertEquals(a.getMoves().get(i).getEvalAfter().format(), a.getMoves().get(i + 1).getEvalBefore().format());
        }
    }

    @Test
    void cancellationStopsEarlyAndReturnsOnlyCompletedMoves() throws Exception {
        String longer = "1. e4 e5 2. Nf3 Nc6 3. Bb5 a6 4. Ba4 Nf6 5. O-O Be7 6. Re1 b5 7. Bb3 d6 *";
        AtomicBoolean cancel = new AtomicBoolean(false);
        GameAnalysis a = new AnalysisService().analyzeGame(new PgnService().parse(longer), settings(),
                (cur, total, label, depth) -> {
                    if (cur >= 3) cancel.set(true);
                }, cancel);
        assertTrue(a.getMoves().size() < 14, "analysis must stop after cancellation, got " + a.getMoves().size());
        assertTrue(a.getMoves().size() >= 3);
        for (MoveAnalysis m : a.getMoves()) {
            assertNotNull(m.getEvalAfter());
        }
    }

    @Test
    void customStartPositionIsAnalysedFromThatPosition() throws Exception {
        String pgn = "[SetUp \"1\"]\n[FEN \"6k1/5ppp/8/8/8/8/5PPP/3R2K1 w - - 0 1\"]\n\n1. Rd8# 1-0";
        GameAnalysis a = new AnalysisService().analyzeGame(new PgnService().parse(pgn), settings(), null, null);
        assertEquals(1, a.getMoves().size());
        assertEquals("Rd8#", a.getMoves().get(0).getSan());
        assertEquals(MoveClassification.BEST, a.getMoves().get(0).getClassification());
        assertEquals(1, a.getMoves().get(0).getEvalAfter().mateSign());
    }

    @Test
    void missingEngineGivesAReadableError() {
        AnalysisSettings s = new AnalysisSettings(AnalysisSettings.Mode.QUICK, "/definitely/not/here/stockfish");
        EngineException ex = assertThrows(EngineException.class, () -> new StockfishService().start(s));
        assertTrue(ex.getMessage().contains("/definitely/not/here/stockfish"), ex.getMessage());
    }

    @Test
    void engineProcessIsShutDownAfterAnalysis() throws Exception {
        long before = stockfishProcessCount();
        new AnalysisService().analyzeGame(new PgnService().parse(FOOLS_MATE), settings(), null, null);
        Thread.sleep(500);
        assertTrue(stockfishProcessCount() <= before, "no stray engine processes may remain");
    }

    private static long stockfishProcessCount() {
        return ProcessHandle.allProcesses()
                .filter(p -> p.info().command().map(c -> c.contains("stockfish")).orElse(false))
                .count();
    }
}

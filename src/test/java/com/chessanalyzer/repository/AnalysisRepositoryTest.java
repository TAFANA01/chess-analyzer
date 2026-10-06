package com.chessanalyzer.repository;

import com.chessanalyzer.model.GameAnalysis;
import com.chessanalyzer.model.MoveClassification;
import com.chessanalyzer.repository.RepositoryException.Kind;
import com.chessanalyzer.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class AnalysisRepositoryTest {

    @TempDir
    Path tmp;

    private AnalysisRepository repo;

    @BeforeEach
    void setUp() {
        repo = new AnalysisRepository(tmp.resolve("analyses"));
    }

    private static RepositoryException kind(Kind expected, org.junit.jupiter.api.function.Executable e) {
        RepositoryException ex = assertThrows(RepositoryException.class, e);
        assertEquals(expected, ex.getKind(), ex.getMessage());
        assertNotNull(ex.getMessage());
        return ex;
    }

    @Test
    void saveAssignsSequentialIdsAndReloadsIdentically() throws Exception {
        GameAnalysis a = TestData.sampleAnalysis("Alice", "Bob");
        File f1 = repo.save(a);
        assertTrue(f1.isFile());
        assertTrue(a.getId().matches("game_\\d{4}_\\d{2}_\\d{2}_001"));

        GameAnalysis b = TestData.sampleAnalysis("Carol", "Dan");
        repo.save(b);
        assertTrue(b.getId().endsWith("_002"));

        GameAnalysis back = repo.loadById(a.getId());
        assertEquals(a.getId(), back.getId());
        assertEquals("Alice", back.getGame().getWhite());
        assertEquals(a.getMoves().size(), back.getMoves().size());
        assertEquals(a.getWhiteAccuracy(), back.getWhiteAccuracy(), 1e-9);
        assertEquals(a.getMoves().get(2).getClassification(), back.getMoves().get(2).getClassification());
        assertEquals(a.getMoves().get(2).getEvalAfter().format(), back.getMoves().get(2).getEvalAfter().format());
        assertEquals(a.getMoves().get(2).getEngineDepth(), back.getMoves().get(2).getEngineDepth());
        assertEquals(GameAnalysis.CURRENT_SCHEMA_VERSION, back.getSchemaVersion());
    }

    @Test
    void survivesNewRepositoryInstanceLikeAnApplicationRestart() throws Exception {
        GameAnalysis a = TestData.sampleAnalysis("Alice", "Bob");
        repo.save(a);
        AnalysisRepository restarted = new AnalysisRepository(tmp.resolve("analyses"));
        assertEquals(1, restarted.listSummaries().summaries().size());
        assertEquals("Alice", restarted.loadById(a.getId()).getGame().getWhite());
    }

    @Test
    void savingLeavesNoTemporaryFilesAndKeepsABackupOnOverwrite() throws Exception {
        GameAnalysis a = TestData.sampleAnalysis("Alice", "Bob");
        repo.save(a);
        a.getGame().setWhite("Alice v2");
        repo.save(a);
        try (Stream<Path> files = Files.list(tmp.resolve("analyses"))) {
            var names = files.map(p -> p.getFileName().toString()).toList();
            assertTrue(names.contains(a.getId() + ".json"));
            assertTrue(names.contains(a.getId() + ".json.bak"), names.toString());
            assertTrue(names.stream().noneMatch(n -> n.endsWith(".tmp")), names.toString());
        }
        assertEquals("Alice v2", repo.loadById(a.getId()).getGame().getWhite());
    }

    @Test
    void rejectsNullAndUnsafeIds() {
        kind(Kind.INVALID_INPUT, () -> repo.save(null));
        GameAnalysis a = TestData.sampleAnalysis("A", "B");
        a.setId("../../etc/passwd");
        kind(Kind.INVALID_INPUT, () -> repo.save(a));
        kind(Kind.INVALID_INPUT, () -> repo.loadById("../x"));
        kind(Kind.INVALID_INPUT, () -> repo.delete("a/b"));
    }

    @Test
    void loadingMissingFileGivesNotFound() {
        kind(Kind.NOT_FOUND, () -> repo.load(new File(tmp.toFile(), "nope.json")));
        kind(Kind.NOT_FOUND, () -> repo.loadById("game_missing"));
    }

    @Test
    void emptyTruncatedAndGarbageFilesAreReportedAsCorrupted() throws Exception {
        Path dir = Files.createDirectories(tmp.resolve("analyses"));
        Files.writeString(dir.resolve("empty.json"), "");
        Files.writeString(dir.resolve("garbage.json"), "this is not json");
        Files.writeString(dir.resolve("array.json"), "[1,2,3]");
        Files.writeString(dir.resolve("nogame.json"), "{\"id\":\"x\",\"moves\":[]}");

        GameAnalysis a = TestData.sampleAnalysis("A", "B");
        File good = repo.save(a);
        String json = Files.readString(good.toPath());
        Files.writeString(dir.resolve("truncated.json"), json.substring(0, json.length() / 2));

        for (String name : new String[]{"empty.json", "garbage.json", "array.json", "nogame.json", "truncated.json"}) {
            kind(Kind.CORRUPTED, () -> repo.load(dir.resolve(name).toFile()));
        }
    }

    @Test
    void incompleteMoveDataIsDetected() throws Exception {
        GameAnalysis a = TestData.sampleAnalysis("A", "B");
        File f = repo.save(a);
        String broken = Files.readString(f.toPath()).replaceFirst("\"fenAfter\"\\s*:\\s*\"[^\"]*\"", "\"fenAfter\" : null");
        Files.writeString(f.toPath(), broken);
        RepositoryException ex = kind(Kind.CORRUPTED, () -> repo.load(f));
        assertTrue(ex.getMessage().contains("move 1"), ex.getMessage());
    }

    @Test
    void newerSchemaVersionIsRefusedNotMisread() throws Exception {
        GameAnalysis a = TestData.sampleAnalysis("A", "B");
        File f = repo.save(a);
        Files.writeString(f.toPath(), Files.readString(f.toPath()).replace("\"schemaVersion\" : 2", "\"schemaVersion\" : 99"));
        kind(Kind.UNSUPPORTED_VERSION, () -> repo.load(f));
    }

    @Test
    void listingReportsBadFilesInsteadOfFailing() throws Exception {
        repo.save(TestData.sampleAnalysis("A", "B"));
        Files.writeString(tmp.resolve("analyses").resolve("bad.json"), "{{{");
        var listing = repo.listSummaries();
        assertEquals(1, listing.summaries().size());
        assertEquals(1, listing.failures().size());
        assertEquals("bad.json", listing.failures().get(0).file().getName());
        assertFalse(listing.failures().get(0).message().isBlank());
    }

    @Test
    void quarantineRemovesFileFromListingWithoutDestroyingIt() throws Exception {
        Path bad = Files.createDirectories(tmp.resolve("analyses")).resolve("bad.json");
        Files.writeString(bad, "{{{");
        File moved = repo.quarantine(bad.toFile());
        assertTrue(moved.getName().endsWith(".corrupt"));
        assertTrue(moved.isFile());
        assertTrue(repo.listSummaries().failures().isEmpty());
    }

    @Test
    void deleteRemovesAnalysisAndBackup() throws Exception {
        GameAnalysis a = TestData.sampleAnalysis("A", "B");
        repo.save(a);
        repo.save(a); // creates .bak
        repo.delete(a.getId());
        assertTrue(repo.listSummaries().summaries().isEmpty());
        assertFalse(Files.exists(tmp.resolve("analyses").resolve(a.getId() + ".json.bak")));
        kind(Kind.NOT_FOUND, () -> repo.delete(a.getId()));
    }

    @Test
    void summariesCarryTheHistoryColumns() throws Exception {
        repo.save(TestData.sampleAnalysis("Alice", "Bob"));
        AnalysisSummary s = repo.listSummaries().summaries().get(0);
        assertEquals("Alice", s.white());
        assertEquals("Bob", s.black());
        assertEquals("1-0", s.result());
        assertEquals("Test Opening", s.opening());
        assertEquals(4, s.plies());
        assertEquals(1, s.whiteMistakes());
        assertEquals(0, s.whiteBlunders());
    }

    @Test
    void unwritableTargetGivesAReadableError() throws Exception {
        // The "directory" is actually a regular file, so it cannot be created or written to.
        Path blocker = tmp.resolve("blocker");
        Files.writeString(blocker, "x");
        AnalysisRepository broken = new AnalysisRepository(blocker.resolve("sub"));
        RepositoryException ex = kind(Kind.IO_FAILURE, () -> broken.save(TestData.sampleAnalysis("A", "B")));
        assertTrue(ex.getMessage().contains("blocker"), ex.getMessage());
    }

    @Test
    void failedSaveNeverDestroysThePreviousVersion() throws Exception {
        GameAnalysis a = TestData.sampleAnalysis("Original", "B");
        File f = repo.save(a);
        String before = Files.readString(f.toPath());

        // An invalid analysis must be rejected before anything on disk is touched.
        GameAnalysis bad = TestData.sampleAnalysis("Replacement", "B");
        bad.setId(a.getId());
        bad.setGame(null);
        kind(Kind.INVALID_INPUT, () -> repo.save(bad));
        assertEquals(before, Files.readString(f.toPath()));
    }

    // ------------------------------------------------------------------ legacy migration

    @Test
    void legacyV1FileIsMigratedAndItsCorruptMateStatisticsRepaired() throws Exception {
        Path dir = Files.createDirectories(tmp.resolve("analyses"));
        Path legacy = dir.resolve("game_2026_10_01_001.json");
        try (var in = getClass().getResourceAsStream("/fixtures/legacy_v1_mate_game.json")) {
            assertNotNull(in, "fixture missing");
            Files.copy(in, legacy, StandardCopyOption.REPLACE_EXISTING);
        }
        String original = Files.readString(legacy);
        assertFalse(original.contains("schemaVersion"), "fixture must be a genuine v1 file");
        assertTrue(original.contains("19999"), "fixture must contain the old corrupt mate loss");

        GameAnalysis a = repo.load(legacy.toFile());

        assertEquals(GameAnalysis.CURRENT_SCHEMA_VERSION, a.getSchemaVersion());
        var last = a.getMoves().get(a.getMoves().size() - 1);
        assertTrue(last.getSan().endsWith("#"));
        assertEquals(0, last.getEvalLossCentipawns(), 0.001, "the mating move must not carry a loss");
        assertEquals(Boolean.TRUE, last.getEvalAfter().getWhiteDeliveredMate());
        assertTrue(a.getWhiteAccuracy() > 85, "White mated in 12 moves; was 48 before the fix, got " + a.getWhiteAccuracy());
        assertTrue(a.getMoves().stream().allMatch(m -> m.getEvalLossCentipawns() <= 2000));
        assertNotEquals(MoveClassification.BLUNDER, last.getClassification());

        // The file on disk is untouched by a load (migration happens in memory until the user re-saves).
        assertEquals(original, Files.readString(legacy));

        // Re-saving writes the new format and reloads without re-migration.
        repo.save(a);
        assertTrue(Files.readString(legacy).contains("\"schemaVersion\" : 2"));
        assertEquals(a.getWhiteAccuracy(), repo.load(legacy.toFile()).getWhiteAccuracy(), 1e-9);
    }
}

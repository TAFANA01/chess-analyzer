package com.chessanalyzer.repository;

import com.chessanalyzer.model.GameAnalysis;
import com.chessanalyzer.repository.RepositoryException.Kind;
import com.chessanalyzer.service.AnalysisRecalculator;
import com.chessanalyzer.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Persists completed analyses as JSON files (one per game) in a directory.
 *
 * <p>Reliability guarantees:
 * <ul>
 *   <li><b>Atomic writes</b>: data is written to a temp file, verified by re-reading it,
 *       and only then moved over the target, so a crash never leaves a half-written file.</li>
 *   <li><b>Backup</b>: a previous version of a file is kept as {@code <id>.json.bak}.</li>
 *   <li><b>Versioned format</b>: old files are migrated on load (see {@link AnalysisMigrator}).</li>
 *   <li><b>No silent failures</b>: every problem raises a {@link RepositoryException} with a user-readable message;
 *       listing never throws because of one bad file, it reports it instead.</li>
 * </ul>
 */
public class AnalysisRepository {

    /** Result of listing the repository: the readable analyses plus every file that could not be read. */
    public record Listing(List<AnalysisSummary> summaries, List<Failure> failures) { }

    /** A file that exists in the repository but could not be loaded. */
    public record Failure(File file, String message) { }

    private static final DateTimeFormatter ID_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy_MM_dd");
    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9_\\-]+");
    private static final String EXT = ".json";

    private static final Logger LOG = Logger.getLogger(AnalysisRepository.class.getName());

    private final Path dir;
    private final AnalysisRecalculator recalculator = new AnalysisRecalculator();

    /** Uses the default directory {@code data/analyses} relative to the working directory (unchanged from V1). */
    public AnalysisRepository() {
        this(Path.of("data", "analyses"));
    }

    public AnalysisRepository(Path dir) {
        this.dir = Objects.requireNonNull(dir, "dir");
    }

    public Path getDirectory() {
        return dir;
    }

    // ------------------------------------------------------------------ save

    /**
     * Saves the analysis, assigning an id like {@code game_2026_09_25_001} if it has none.
     *
     * @return the file written
     * @throws RepositoryException if the analysis is invalid or the disk write/verification fails
     */
    public synchronized File save(GameAnalysis analysis) throws IOException {
        if (analysis == null || analysis.getGame() == null) {
            throw new RepositoryException(Kind.INVALID_INPUT, "There is no analysis to save.");
        }
        ensureDirectory();
        if (analysis.getId() == null || analysis.getId().isBlank()) {
            analysis.setId(nextId());
        } else if (!SAFE_ID.matcher(analysis.getId()).matches()) {
            throw new RepositoryException(Kind.INVALID_INPUT, "Invalid analysis id: " + analysis.getId());
        }
        analysis.setSchemaVersion(GameAnalysis.CURRENT_SCHEMA_VERSION);

        Path target = fileFor(analysis.getId());
        Path temp = dir.resolve("." + analysis.getId() + ".tmp");
        try {
            JsonUtil.writeToFile(analysis, temp.toFile());
            verifyReadable(temp, analysis);

            if (Files.exists(target)) {
                Files.copy(target, dir.resolve(analysis.getId() + EXT + ".bak"), StandardCopyOption.REPLACE_EXISTING);
            }
            moveReplacing(temp, target);
        } catch (RepositoryException e) {
            deleteQuietly(temp);
            throw e;
        } catch (IOException e) {
            deleteQuietly(temp);
            LOG.log(Level.SEVERE, "Could not save analysis " + analysis.getId() + " to " + target, e);
            throw new RepositoryException(Kind.IO_FAILURE,
                    "The analysis could not be saved to " + target.toAbsolutePath() + ": " + describe(e), e);
        }
        LOG.info("Saved analysis " + analysis.getId() + " (" + analysis.getMoves().size() + " moves)");
        return target.toFile();
    }

    /** Re-reads the temp file to make sure what we are about to publish is a complete, loadable analysis. */
    private void verifyReadable(Path temp, GameAnalysis original) throws IOException {
        try {
            GameAnalysis copy = JsonUtil.readFromFile(temp.toFile(), GameAnalysis.class);
            if (copy.getGame() == null || copy.getMoves().size() != original.getMoves().size()) {
                throw new RepositoryException(Kind.IO_FAILURE,
                        "Verification after writing failed: the saved data does not match the analysis. Nothing was overwritten.");
            }
        } catch (JsonProcessingException e) {
            throw new RepositoryException(Kind.IO_FAILURE,
                    "Verification after writing failed: " + e.getOriginalMessage() + ". Nothing was overwritten.", e);
        }
    }

    private void moveReplacing(Path temp, Path target) throws IOException {
        try {
            Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    // ------------------------------------------------------------------ load

    /**
     * Loads an analysis file, migrating old formats and validating the content.
     *
     * @throws RepositoryException {@link Kind#NOT_FOUND}, {@link Kind#CORRUPTED},
     *                             {@link Kind#UNSUPPORTED_VERSION} or {@link Kind#IO_FAILURE}
     */
    public GameAnalysis load(File file) throws IOException {
        if (file == null || !file.isFile()) {
            throw new RepositoryException(Kind.NOT_FOUND, "The analysis file does not exist: " + file);
        }
        if (file.length() == 0) {
            throw new RepositoryException(Kind.CORRUPTED, file.getName() + " is empty.");
        }
        try {
            JsonNode tree = JsonUtil.mapper().readTree(file);
            if (!(tree instanceof ObjectNode root)) {
                throw new RepositoryException(Kind.CORRUPTED, file.getName() + " is not a valid analysis file.");
            }
            AnalysisMigrator.Result migration = AnalysisMigrator.migrate(root);
            GameAnalysis analysis = JsonUtil.mapper().treeToValue(root, GameAnalysis.class);
            validate(analysis, file);
            if (migration.migrated()) {
                LOG.info("Migrated " + file.getName() + " to schema v" + GameAnalysis.CURRENT_SCHEMA_VERSION
                        + "; recalculating derived statistics");
                recalculator.recalculate(analysis);
            }
            return analysis;
        } catch (RepositoryException e) {
            throw e;
        } catch (JsonProcessingException e) {
            LOG.log(Level.WARNING, "Corrupted analysis file " + file, e);
            throw new RepositoryException(Kind.CORRUPTED,
                    file.getName() + " is corrupted or incomplete (" + e.getOriginalMessage() + ").", e);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Could not read analysis file " + file, e);
            throw new RepositoryException(Kind.IO_FAILURE, "Could not read " + file.getName() + ": " + describe(e), e);
        }
    }

    public GameAnalysis loadById(String id) throws IOException {
        if (id == null || !SAFE_ID.matcher(id).matches()) {
            throw new RepositoryException(Kind.INVALID_INPUT, "Invalid analysis id: " + id);
        }
        return load(fileFor(id).toFile());
    }

    private void validate(GameAnalysis a, File file) throws RepositoryException {
        if (a.getGame() == null) {
            throw new RepositoryException(Kind.CORRUPTED, file.getName() + " has no game data.");
        }
        if (a.getMoves().isEmpty()) {
            throw new RepositoryException(Kind.CORRUPTED, file.getName() + " contains no analysed moves.");
        }
        for (int i = 0; i < a.getMoves().size(); i++) {
            var m = a.getMoves().get(i);
            if (m == null || m.getFenAfter() == null || m.getSan() == null) {
                throw new RepositoryException(Kind.CORRUPTED,
                        file.getName() + " is incomplete: move " + (i + 1) + " has missing data.");
            }
        }
        if (a.getId() == null || a.getId().isBlank()) {
            String name = file.getName();
            a.setId(name.endsWith(EXT) ? name.substring(0, name.length() - EXT.length()) : name);
        }
    }

    // ------------------------------------------------------------------ list / delete

    /** Lists all saved analysis files, most recent first (by analysis time stored in the file name order, then mtime). */
    public List<File> listSavedAnalyses() {
        File[] files = dir.toFile().listFiles((d, name) -> name.endsWith(EXT) && !name.startsWith("."));
        List<File> result = new ArrayList<>();
        if (files != null) {
            result.addAll(List.of(files));
        }
        result.sort(Comparator.comparingLong(File::lastModified).reversed());
        return result;
    }

    /** Loads every saved analysis into a summary; unreadable files are reported, never thrown. */
    public Listing listSummaries() {
        List<AnalysisSummary> summaries = new ArrayList<>();
        List<Failure> failures = new ArrayList<>();
        for (File f : listSavedAnalyses()) {
            try {
                summaries.add(AnalysisSummary.of(load(f), f));
            } catch (IOException e) {
                failures.add(new Failure(f, e.getMessage()));
            }
        }
        summaries.sort(Comparator.comparing(AnalysisSummary::analyzedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return new Listing(summaries, failures);
    }

    /** Deletes an analysis (and its backup). */
    public synchronized void delete(String id) throws IOException {
        if (id == null || !SAFE_ID.matcher(id).matches()) {
            throw new RepositoryException(Kind.INVALID_INPUT, "Invalid analysis id: " + id);
        }
        Path file = fileFor(id);
        if (!Files.exists(file)) {
            throw new RepositoryException(Kind.NOT_FOUND, "Analysis " + id + " no longer exists.");
        }
        try {
            Files.delete(file);
            Files.deleteIfExists(dir.resolve(id + EXT + ".bak"));
        } catch (IOException e) {
            throw new RepositoryException(Kind.IO_FAILURE, "Could not delete " + id + ": " + describe(e), e);
        }
    }

    /** Renames an unreadable file to {@code *.corrupt} so it stops appearing in lists but is never destroyed. */
    public synchronized File quarantine(File file) throws IOException {
        Path source = file.toPath();
        Path target = source.resolveSibling(file.getName() + ".corrupt");
        try {
            return Files.move(source, target, StandardCopyOption.REPLACE_EXISTING).toFile();
        } catch (IOException e) {
            throw new RepositoryException(Kind.IO_FAILURE, "Could not quarantine " + file.getName() + ": " + describe(e), e);
        }
    }

    // ------------------------------------------------------------------ helpers

    private Path fileFor(String id) {
        return dir.resolve(id + EXT);
    }

    private void ensureDirectory() throws RepositoryException {
        try {
            Files.createDirectories(dir);
            if (!Files.isWritable(dir)) {
                throw new RepositoryException(Kind.IO_FAILURE,
                        "The folder " + dir.toAbsolutePath() + " is not writable. Check its permissions.");
            }
        } catch (IOException e) {
            throw new RepositoryException(Kind.IO_FAILURE,
                    "Could not create the data folder " + dir.toAbsolutePath() + ": " + describe(e), e);
        }
    }

    /** Must be called while holding the repository lock. */
    private String nextId() {
        String prefix = "game_" + LocalDate.now().format(ID_DATE_FORMAT) + "_";
        File[] existing = dir.toFile().listFiles((d, name) -> name.startsWith(prefix) && name.endsWith(EXT));
        int max = 0;
        if (existing != null) {
            for (File f : existing) {
                String name = f.getName();
                String suffix = name.substring(prefix.length(), name.length() - EXT.length());
                try {
                    max = Math.max(max, Integer.parseInt(suffix));
                } catch (NumberFormatException ignored) {
                    // not one of our generated ids
                }
            }
        }
        return prefix + String.format("%03d", max + 1);
    }

    private static void deleteQuietly(Path p) {
        try {
            Files.deleteIfExists(p);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Could not remove temporary file " + p, e);
        }
    }

    private static String describe(IOException e) {
        return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
    }
}

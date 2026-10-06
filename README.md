# Chess Game Analyzer (V1)

Offline JavaFX desktop app: import a PGN, run a deep Stockfish analysis on
every move, and generate a professional final report. No chess board, no
interactive play -- this is an analysis and reporting tool only.

## Requirements

- Java 22 (JDK)
- Maven 3.9+
- A local Stockfish executable ([stockfishchess.org/download](https://stockfishchess.org/download/))
  - The app tries to auto-detect common install paths on startup; otherwise
    browse to it manually on the main screen.

## Build & run

```bash
mvn clean compile
mvn javafx:run
```

## Package a runnable jar

```bash
mvn clean package
java -jar target/chess-game-analyzer-1.0.0.jar
```

## Project layout

```
com.chessanalyzer
├── Main.java                 # JavaFX entry point + screen navigator
├── controller/                # MainController, AnalyzeController, ReportController
├── model/                      # ChessGame, MoveAnalysis, GameAnalysis, Evaluation, ...
├── service/
│   ├── PgnService              # PGN validation + reconstruction (chesslib)
│   ├── StockfishService         # UCI process lifecycle, one position at a time
│   ├── AnalysisService          # drives the full per-move pipeline
│   ├── AccuracyService          # win%-based accuracy (not naive 100 - avg CPL)
│   ├── MoveClassificationService # centralized BEST..BLUNDER thresholds
│   ├── GamePhaseService         # opening / middlegame / endgame split
│   ├── CommentService           # human-readable explanations + reason tags
│   ├── ReportService            # critical moments + narrative report sections
│   └── ReportExportService      # TXT / JSON export
├── repository/AnalysisRepository # saves each analysis as data/analyses/*.json
└── util/                        # EvaluationUtil, JsonUtil, FileUtil,
                                  # SanFormatter, UciMoveParser
```

## Status / verification

The V1 sources were compile-checked with `javac` against chesslib 1.3.7 (built from the
GitHub tag), JavaFX, Jackson and JUnit; `SanFormatter` and `UciMoveParser` compile and work
against real Stockfish 16 output. The test suite (`mvn test`) covers PGN parsing, evaluation
and loss math, classification, accuracy, persistence (corruption, migration, atomic writes) and
an end-to-end PGN -> Stockfish -> save -> reload pipeline. Integration tests are skipped
automatically when no Stockfish is found; force one with `-Dstockfish.path=/path/to/stockfish`.

### Analysis board

After an analysis, the Report screen has a **Review Board** button that opens the interactive board.

* One `GameNavigator` (package `board`) holds the current position; the board, move list, evaluation bar and
  panel, move-information card, evaluation graph, slider and buttons all observe it. It reads FENs, evaluations
  and best moves already stored in `MoveAnalysis` - no engine call happens while navigating.
* Keyboard: Left / Right previous / next, Home / End first / last, Space play / pause.
* Arrows: green = the engine's better move after an inaccuracy, mistake or blunder; blue = engine suggestion
  for the side to move (toggle **Show best move**); red = the move that was played (shown by **Before move**,
  which displays the position the move was actually played from).
* The board is read-only; pieces cannot be moved and the analysis is never modified.
* Pieces are drawn with chess glyphs from the first installed font that has them (Segoe UI Symbol, Apple Symbols,
  DejaVu Sans, ...).

### Tests

`mvn test` runs the unit tests and the `*IT` tests. `AnalysisBoardViewIT` needs a display (on a headless Linux box
use `xvfb-run mvn test`); the Stockfish tests need an engine (`-Dstockfish.path=...`). Each skips itself when its
requirement is missing.

### Saved-analysis format

Files in `data/analyses/*.json` carry a `schemaVersion` (currently 2). Older files (no version)
are migrated in memory on load and their loss/accuracy/statistics are recalculated from the stored
evaluations; the file on disk is only rewritten when you save again. Writes are atomic (temp file +
verify + move) and keep a `.bak` of the previous version.

### Move classification

Thresholds live in `model/ClassificationThresholds` (defaults 20 / 60 / 120 / 250 cp). The
engine's top move is always BEST; any other move is at most EXCELLENT. BRILLIANT = a near-best
move that gives up >= 200cp of material three plies deep while staying sound. See
`MoveClassificationService` for the full documented logic.

## What's deliberately NOT here (per spec)

No chess board, no interactive pieces, no move legality visualization, no
accounts/cloud/online APIs, no puzzle generator, no SQLite. V1 is strictly
PGN -> Stockfish analysis -> report.

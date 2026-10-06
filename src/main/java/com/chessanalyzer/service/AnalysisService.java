package com.chessanalyzer.service;

import com.chessanalyzer.model.AnalysisSettings;
import com.chessanalyzer.model.ChessGame;
import com.chessanalyzer.model.GameAnalysis;
import com.chessanalyzer.model.MoveAnalysis;
import com.chessanalyzer.model.MoveClassification;
import com.chessanalyzer.util.EvaluationUtil;
import com.chessanalyzer.util.FenUtil;
import com.chessanalyzer.util.MaterialUtil;
import com.chessanalyzer.util.SanFormatter;
import com.chessanalyzer.util.UciMoveParser;
import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.move.Move;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Runs the full "deep analysis" pipeline described in the project spec:
 * for every played move, analyze the position before it, play the move,
 * analyze the resulting position, compute the evaluation loss, classify
 * the move, and attach a human-readable comment for notable moves.
 *
 * This is the single place that drives the engine across an entire game;
 * StockfishService only knows how to analyze one position at a time.
 */
public class AnalysisService {

    private final MoveClassificationService classificationService = new MoveClassificationService();
    private final CommentService commentService = new CommentService();
    private final GamePhaseService phaseService = new GamePhaseService();
    private final AccuracyService accuracyService = new AccuracyService();

    /**
     * @param cancelFlag polled between moves (and inside each engine search); when set,
     *                   analysis stops after the current move and returns whatever was completed.
     */
    public GameAnalysis analyzeGame(ChessGame game, AnalysisSettings settings,
                                     AnalysisProgressListener progressListener,
                                     AtomicBoolean cancelFlag) throws EngineException {

        GameAnalysis analysis = new GameAnalysis();
        analysis.setGame(game);
        analysis.setSettings(settings);

        StockfishService engine = new StockfishService();
        try {
            engine.start(settings);

            Board board = new Board();
            if (game.getStartFen() != null && !game.getStartFen().isBlank()) {
                board.loadFromFen(game.getStartFen());
            }
            // Each position is searched once: the "after" position of ply n is the "before" position of ply n+1.
            Map<String, EngineAnalysisResult> positionCache = new HashMap<>();
            List<String> sanMoves = game.getMovesSan();
            int total = sanMoves.size();

            for (int i = 0; i < total; i++) {
                if (cancelFlag != null && cancelFlag.get()) {
                    break;
                }

                String sanToken = sanMoves.get(i);
                boolean whiteToMove = board.getSideToMove() == Side.WHITE;
                String fenBefore = board.getFen();

                // 1. Analyze the position before the played move.
                EngineAnalysisResult beforeResult = analyzeCached(engine, positionCache, fenBefore,
                        whiteToMove, settings, cancelFlag);
                if (cancelFlag != null && cancelFlag.get()) {
                    break; // a cancelled search returns a partial result; never record it
                }

                int materialBefore = MaterialUtil.balance(board, board.getSideToMove());
                Side mover = board.getSideToMove();

                String bestMoveSan = null;
                String pvSan = null;
                if (beforeResult.getBestMoveUci() != null) {
                    Move bestMove = UciMoveParser.resolve(board, beforeResult.getBestMoveUci());
                    if (bestMove != null) {
                        bestMoveSan = SanFormatter.toSan(copyBoard(board), bestMove);
                        List<Move> pvMoves = resolvePv(board, beforeResult.getPrincipalVariationUci());
                        pvSan = SanFormatter.toSanSequence(copyBoard(board), pvMoves, 6);
                    }
                }

                // 2. Play the actual move.
                if (!board.doMove(sanToken)) {
                    throw new EngineException("Move " + sanToken + " could not be replayed from position " + fenBefore);
                }
                // The move just played is the last entry in chesslib's backup stack (no SAN round-trip needed).
                Move playedMove = board.getBackup().getLast().getMove();
                String playedUci = toUci(playedMove);
                String fenAfter = board.getFen();
                boolean nowWhiteToMove = board.getSideToMove() == Side.WHITE;

                // 3. Analyze the resulting position.
                EngineAnalysisResult afterResult = analyzeCached(engine, positionCache, fenAfter,
                        nowWhiteToMove, settings, cancelFlag);
                if (cancelFlag != null && cancelFlag.get()) {
                    break;
                }

                double sacrificed = materialSacrificedCp(fenAfter, afterResult.getPrincipalVariationUci(),
                        mover, materialBefore);

                MoveAnalysis ma = new MoveAnalysis();
                ma.setMoveNumber(FenUtil.fullmoveNumber(fenBefore));
                ma.setWhiteToMove(whiteToMove);
                ma.setSan(sanToken);
                ma.setUci(playedUci);
                ma.setFenBefore(fenBefore);
                ma.setFenAfter(fenAfter);
                ma.setEvalBefore(beforeResult.getEvaluation());
                ma.setEvalAfter(afterResult.getEvaluation());
                ma.setBestMoveSan(bestMoveSan);
                ma.setBestMoveUci(beforeResult.getBestMoveUci());
                ma.setPrincipalVariationSan(pvSan);
                ma.setEngineDepth(beforeResult.getDepthReached());
                ma.setEngineDepthAfter(afterResult.getDepthReached());

                // 4-6. Compare expected vs actual evaluation, compute loss, classify.
                // Playing the engine's top move cannot lose anything by definition; the two
                // independent searches would otherwise leak search noise into the loss.
                boolean playedBest = playedUci.equalsIgnoreCase(beforeResult.getBestMoveUci());
                double loss = playedBest ? 0
                        : EvaluationUtil.lossForMover(beforeResult.getEvaluation(), afterResult.getEvaluation(), whiteToMove);
                ma.setEvalLossCentipawns(loss);

                MoveClassification classification = classificationService.classify(
                        beforeResult.getEvaluation(), afterResult.getEvaluation(), whiteToMove,
                        playedUci, beforeResult.getBestMoveUci(), sacrificed);
                ma.setClassification(classification);

                analysis.getMoves().add(ma);

                if (progressListener != null) {
                    progressListener.onProgress(i + 1, total, ma.moveLabel(), settings.getDepth());
                }
            }

            finalizeAnalysis(analysis);

        } finally {
            engine.quit();
        }

        return analysis;
    }

    /**
     * Derives everything that can be computed from the stored per-move evaluations alone
     * (phases, comments, phase stats, accuracy, classification counts). Safe to call again
     * on a loaded analysis - e.g. after a schema migration or a thresholds change - without
     * touching the engine. Existing derived data is replaced.
     */
    public void finalizeAnalysis(GameAnalysis analysis) {
        analysis.getPhaseStats().clear();
        analysis.setMoveStatistics(new com.chessanalyzer.model.MoveStatistics());

        // Phases are assigned before comments: reason categorization uses the phase of a move.
        phaseService.assignPhases(analysis.getMoves());
        for (MoveAnalysis m : analysis.getMoves()) {
            commentService.annotate(m);
        }

        analysis.getPhaseStats().addAll(phaseService.computePhaseStats(analysis.getMoves(), accuracyService));

        AccuracyResult accuracy = accuracyService.computeForGame(analysis.getMoves());
        analysis.setWhiteAccuracy(accuracy.whiteAccuracy());
        analysis.setBlackAccuracy(accuracy.blackAccuracy());

        for (MoveAnalysis m : analysis.getMoves()) {
            analysis.getMoveStatistics().record(m.isWhiteToMove(), m.getClassification());
        }
    }

    /** chesslib's Board has no public clone(); rebuild an independent copy from FEN instead. */
    private Board copyBoard(Board source) {
        Board copy = new Board();
        copy.loadFromFen(source.getFen());
        return copy;
    }

    private EngineAnalysisResult analyzeCached(StockfishService engine, Map<String, EngineAnalysisResult> cache,
                                               String fen, boolean whiteToMove, AnalysisSettings settings,
                                               AtomicBoolean cancelFlag) throws EngineException {
        EngineAnalysisResult cached = cache.get(fen);
        if (cached != null) {
            return cached;
        }
        EngineAnalysisResult result = engine.analyzePosition(
                fen, whiteToMove, settings.getDepth(), settings.getMoveTimeLimitMs(), cancelFlag);
        // Never cache a search that was interrupted by cancellation.
        if (cancelFlag == null || !cancelFlag.get()) {
            cache.put(fen, result);
        }
        return result;
    }

    /**
     * Net material (cp) the mover has given up three plies after the played move
     * (own move, opponent's best reply, own follow-up along the engine PV),
     * compared with the balance before the move. Positive = a real sacrifice.
     */
    private double materialSacrificedCp(String fenAfterMove, List<String> pvAfterMove, Side mover, int balanceBefore) {
        if (pvAfterMove == null || pvAfterMove.isEmpty()) {
            return 0;
        }
        Board scratch = new Board();
        scratch.loadFromFen(fenAfterMove);
        int plies = Math.min(2, pvAfterMove.size());
        for (int i = 0; i < plies; i++) {
            Move m = UciMoveParser.resolve(scratch, pvAfterMove.get(i));
            if (m == null) {
                return 0;
            }
            try {
                scratch.doMove(m);
            } catch (RuntimeException e) {
                return 0;
            }
        }
        int balanceAfter = MaterialUtil.balance(scratch, mover);
        return Math.max(0, balanceBefore - balanceAfter);
    }

    private List<Move> resolvePv(Board position, List<String> pvUci) {
        Board scratch = copyBoard(position);
        return pvUci.stream()
                .map(uci -> {
                    Move m = UciMoveParser.resolve(scratch, uci);
                    if (m != null) {
                        try {
                            scratch.doMove(m);
                        } catch (RuntimeException ignored) {
                            // stop resolving further PV moves if something goes wrong
                        }
                    }
                    return m;
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private String toUci(Move m) {
        StringBuilder sb = new StringBuilder();
        sb.append(m.getFrom().toString().toLowerCase());
        sb.append(m.getTo().toString().toLowerCase());
        Piece promo = m.getPromotion();
        if (promo != null && promo != Piece.NONE) {
            sb.append(promoLetter(promo));
        }
        return sb.toString();
    }

    private char promoLetter(Piece p) {
        return switch (p.getPieceType()) {
            case QUEEN -> 'q';
            case ROOK -> 'r';
            case BISHOP -> 'b';
            case KNIGHT -> 'n';
            default -> ' ';
        };
    }
}

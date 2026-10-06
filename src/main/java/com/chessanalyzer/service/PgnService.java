package com.chessanalyzer.service;

import com.chessanalyzer.model.ChessGame;
import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.move.Move;
import com.github.bhlangonijr.chesslib.move.MoveList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses, validates and replays PGN text (one or several games).
 *
 * <p>Supported: standard seven-tag roster plus Elo/TimeControl/ECO/Opening, {@code [SetUp]/[FEN]}
 * start positions, {@code {}} and {@code ;} comments, {@code %} escape lines, NAGs, annotation
 * suffixes ({@code !?}), nested variations (skipped - main line only), {@code 0-0} castling spelling,
 * Windows/Unix line endings and a UTF-8 BOM.
 *
 * <p>Move legality is verified by replaying every move with chesslib; the first illegal or
 * unparsable move is reported with its move number and side.
 */
public class PgnService {

    private static final Pattern TAG_LINE = Pattern.compile("^\\s*\\[(\\w+)\\s+\"((?:[^\"\\\\]|\\\\.)*)\"\\s*\\]\\s*$");
    private static final Pattern MOVE_NUMBER_PREFIX = Pattern.compile("^\\d+\\.+");
    private static final Pattern NAG = Pattern.compile("^\\$\\d+$");
    private static final Pattern RESULT = Pattern.compile("1-0|0-1|1/2-1/2|\\*");
    private static final Pattern ANNOTATION_SUFFIX = Pattern.compile("[!?]+$");

    /**
     * Parses the first game in the text. Kept for compatibility with V1 callers.
     *
     * @throws PgnValidationException if the PGN is empty, malformed, or the first game contains an illegal move
     */
    public ChessGame parse(String rawPgn) throws PgnValidationException {
        PgnParseResult result = parseAll(rawPgn);
        if (!result.hasGames()) {
            // parseAll only returns no games and no errors for blank input
            throw new PgnValidationException(result.hasErrors()
                    ? result.errors().get(0).message()
                    : "The PGN is empty. Paste or import a game before analyzing.");
        }
        return result.games().get(0);
    }

    /**
     * Parses every game found in the text.
     *
     * @throws PgnValidationException only if the input is null/blank or contains no game at all
     */
    public PgnParseResult parseAll(String rawPgn) throws PgnValidationException {
        if (rawPgn == null || rawPgn.isBlank()) {
            throw new PgnValidationException("The PGN is empty. Paste or import a game before analyzing.");
        }
        List<String> chunks = splitGames(rawPgn);
        if (chunks.isEmpty()) {
            throw new PgnValidationException("No game was found. A PGN needs header tags and/or a list of moves.");
        }

        List<ChessGame> games = new ArrayList<>();
        List<PgnParseResult.GameError> errors = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            ChessGame header = new ChessGame();
            try {
                games.add(parseSingle(chunks.get(i), header));
            } catch (PgnValidationException e) {
                errors.add(new PgnParseResult.GameError(i + 1, header.getWhite(), header.getBlack(), e.getMessage()));
            }
        }
        if (games.isEmpty() && chunks.size() == 1) {
            // Single-game input: surface the specific error directly, as V1 did.
            throw new PgnValidationException(errors.get(0).message());
        }
        return new PgnParseResult(games, errors);
    }

    // ------------------------------------------------------------------ splitting

    /** Splits a file into per-game text blocks: a tag line after movetext starts a new game. */
    List<String> splitGames(String text) {
        String normalized = text.replace("\uFEFF", "").replace("\r\n", "\n").replace('\r', '\n');
        List<String> games = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inMovetext = false;
        int braceDepth = 0;

        for (String line : normalized.split("\n", -1)) {
            boolean tagLine = braceDepth == 0 && TAG_LINE.matcher(line).matches();
            if (tagLine && inMovetext) {
                addIfContent(games, current);
                current = new StringBuilder();
                inMovetext = false;
            }
            if (!tagLine && !line.isBlank()) {
                inMovetext = true;
            }
            current.append(line).append('\n');
            for (char c : line.toCharArray()) {
                if (c == '{') braceDepth++;
                else if (c == '}' && braceDepth > 0) braceDepth--;
            }
        }
        addIfContent(games, current);
        return games;
    }

    private void addIfContent(List<String> games, StringBuilder sb) {
        if (!sb.toString().isBlank()) {
            games.add(sb.toString());
        }
    }

    // ------------------------------------------------------------------ one game

    private ChessGame parseSingle(String gameText, ChessGame game) throws PgnValidationException {
        game.setRawPgn(gameText.strip());

        StringBuilder movetext = new StringBuilder();
        Map<String, String> tags = new HashMap<>();
        for (String line : gameText.split("\n")) {
            Matcher m = TAG_LINE.matcher(line);
            if (m.matches()) {
                String value = m.group(2).replace("\\\"", "\"").replace("\\\\", "\\");
                tags.put(m.group(1), value);
                applyTag(game, m.group(1), value);
            } else {
                movetext.append(line).append('\n');
            }
        }

        Board board = new Board();
        String fenTag = tags.get("FEN");
        if (fenTag != null && !"0".equals(tags.get("SetUp"))) {
            try {
                board.loadFromFen(fenTag);
            } catch (RuntimeException e) {
                throw new PgnValidationException("The [FEN] tag is not a valid position: \"" + fenTag + "\".", e);
            }
            game.setStartFen(board.getFen());
        }

        List<String> sanTokens = tokenize(movetext.toString());
        if (sanTokens.isEmpty()) {
            throw new PgnValidationException("No moves were found in this game.");
        }

        int plyOffset = board.getSideToMove() == com.github.bhlangonijr.chesslib.Side.BLACK ? 1 : 0;
        for (int i = 0; i < sanTokens.size(); i++) {
            String san = sanTokens.get(i);
            Throwable cause = null;
            boolean ok = false;
            try {
                Move move = resolveLegalMove(board, san);
                ok = move != null && board.doMove(move, true);
            } catch (RuntimeException e) {
                cause = e;
            }
            if (!ok) {
                int ply = i + plyOffset;
                String side = (ply % 2 == 0) ? "White" : "Black";
                throw new PgnValidationException("Invalid move " + (ply / 2 + 1) + (ply % 2 == 0 ? "." : "...")
                        + " (" + side + "): \"" + san + "\" is not a legal move in this position.", cause);
            }
            game.getMovesSan().add(san);
        }
        return game;
    }

    /**
     * Converts SAN to a move and accepts it only if it is in the position's legal-move list.
     * chesslib's SAN parser alone is too lenient (e.g. it resolves "e5" from the start position
     * to an illegal three-square pawn push), so legality is checked explicitly.
     *
     * @return the legal move, or null if the SAN does not denote one
     */
    private Move resolveLegalMove(Board board, String san) {
        MoveList scratch = new MoveList(board.getFen());
        scratch.addSanMove(san, true, true);
        Move candidate = scratch.removeLast();
        return board.legalMoves().contains(candidate) ? candidate : null;
    }

    private void applyTag(ChessGame game, String tag, String value) {
        switch (tag) {
            case "White" -> game.setWhite(blankToUnknown(value));
            case "Black" -> game.setBlack(blankToUnknown(value));
            case "Result" -> game.setResult(value.isBlank() ? "*" : value);
            case "Date" -> game.setDate(value);
            case "Event" -> game.setEvent(value);
            case "Site" -> game.setSite(value);
            case "Round" -> game.setRound(value);
            case "ECO" -> game.setEco(value);
            case "Opening" -> game.setOpening(value);
            case "TimeControl" -> game.setTimeControl(value);
            case "WhiteElo" -> game.setWhiteElo(parseElo(value));
            case "BlackElo" -> game.setBlackElo(parseElo(value));
            default -> { /* other tags stay available through rawPgn */ }
        }
    }

    private Integer parseElo(String value) {
        try {
            int elo = Integer.parseInt(value.trim());
            return (elo > 0 && elo < 4500) ? elo : null;
        } catch (NumberFormatException e) {
            return null; // "?" or "-" or garbage: unknown
        }
    }

    private String blankToUnknown(String value) {
        return (value == null || value.isBlank()) ? "Unknown" : value;
    }

    // ------------------------------------------------------------------ tokenizer

    /**
     * Turns movetext into the ordered list of main-line SAN tokens. Comments, variations
     * (to any nesting depth), NAGs, move numbers, annotation suffixes and result markers are removed.
     */
    List<String> tokenize(String movetext) throws PgnValidationException {
        StringBuilder clean = new StringBuilder(movetext.length());
        int variationDepth = 0;
        boolean atLineStart = true;
        for (int i = 0; i < movetext.length(); i++) {
            char c = movetext.charAt(i);
            if (c == '{') {
                int end = movetext.indexOf('}', i);
                if (end < 0) {
                    throw new PgnValidationException("Unterminated comment: a '{' has no closing '}'.");
                }
                i = end;
                clean.append(' ');
            } else if (c == ';') {
                int end = movetext.indexOf('\n', i);
                i = (end < 0) ? movetext.length() : end - 1;
            } else if (c == '%' && atLineStart) {
                int end = movetext.indexOf('\n', i);
                i = (end < 0) ? movetext.length() : end - 1;
            } else if (c == '(') {
                variationDepth++;
            } else if (c == ')') {
                if (variationDepth == 0) {
                    throw new PgnValidationException("Unbalanced parentheses: found ')' without a matching '('.");
                }
                variationDepth--;
            } else if (variationDepth == 0) {
                clean.append(c);
            }
            atLineStart = (c == '\n');
        }
        if (variationDepth != 0) {
            throw new PgnValidationException("Unbalanced parentheses: a variation '(' was never closed.");
        }

        List<String> tokens = new ArrayList<>();
        for (String raw : clean.toString().trim().split("\\s+")) {
            String token = raw;
            if (token.isBlank() || NAG.matcher(token).matches()) continue;
            token = MOVE_NUMBER_PREFIX.matcher(token).replaceFirst("");
            if (token.isBlank() || token.matches("\\.+")) continue;
            if (RESULT.matcher(token).matches()) continue;
            token = ANNOTATION_SUFFIX.matcher(token).replaceFirst("");
            token = token.replace("0-0-0", "O-O-O").replace("0-0", "O-O");
            if (token.isBlank()) continue;
            if (token.equals("--") || token.equals("Z0")) {
                throw new PgnValidationException("Null moves ('--') are not supported.");
            }
            tokens.add(token);
        }
        return tokens;
    }
}

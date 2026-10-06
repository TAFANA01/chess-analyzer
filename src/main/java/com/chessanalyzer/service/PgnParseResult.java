package com.chessanalyzer.service;

import com.chessanalyzer.model.ChessGame;

import java.util.List;

/**
 * Outcome of parsing a PGN file that may contain several games. A malformed game
 * does not discard the valid ones: it is reported in {@link #errors()} instead.
 */
public record PgnParseResult(List<ChessGame> games, List<GameError> errors) {

    /** @param gameNumber 1-based position of the game in the file */
    public record GameError(int gameNumber, String white, String black, String message) {
        public String describe() {
            return "Game " + gameNumber + " (" + white + " vs " + black + "): " + message;
        }
    }

    public boolean hasGames() {
        return !games.isEmpty();
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}

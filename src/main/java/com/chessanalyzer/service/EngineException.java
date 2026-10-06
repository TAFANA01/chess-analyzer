package com.chessanalyzer.service;

/** Thrown when the Stockfish process fails to start, times out, or dies unexpectedly. */
public class EngineException extends Exception {

    public EngineException(String message) {
        super(message);
    }

    public EngineException(String message, Throwable cause) {
        super(message, cause);
    }
}

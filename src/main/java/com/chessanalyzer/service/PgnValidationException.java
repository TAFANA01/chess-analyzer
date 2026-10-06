package com.chessanalyzer.service;

/** Thrown when a PGN is empty, malformed, or contains an illegal move. */
public class PgnValidationException extends Exception {

    public PgnValidationException(String message) {
        super(message);
    }

    public PgnValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}

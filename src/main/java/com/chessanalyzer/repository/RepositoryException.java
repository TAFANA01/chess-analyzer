package com.chessanalyzer.repository;

import java.io.IOException;

/**
 * Persistence failure with a message that is safe to show to the user.
 * Extends {@link IOException} so existing callers that catch IOException keep working.
 */
public class RepositoryException extends IOException {

    public enum Kind { NOT_FOUND, CORRUPTED, UNSUPPORTED_VERSION, IO_FAILURE, INVALID_INPUT }

    private final Kind kind;

    public RepositoryException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public RepositoryException(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    public Kind getKind() {
        return kind;
    }
}

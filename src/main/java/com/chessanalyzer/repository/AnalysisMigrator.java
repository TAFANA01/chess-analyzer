package com.chessanalyzer.repository;

import com.chessanalyzer.model.GameAnalysis;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Upgrades persisted analysis JSON from older layouts to {@link GameAnalysis#CURRENT_SCHEMA_VERSION}.
 * Works on the raw JSON tree so that old files can be read before they are bound to the current classes.
 *
 * <ul>
 *   <li>v1 (no {@code schemaVersion} field): checkmate evaluations were stored as {@code mateIn: 0}
 *       without saying who delivered the mate, and loss/accuracy were computed from them incorrectly.
 *       The migrator repairs the evaluation and flags the file so derived values are recalculated.</li>
 * </ul>
 */
final class AnalysisMigrator {

    record Result(boolean migrated) { }

    private AnalysisMigrator() {
    }

    static Result migrate(ObjectNode root) throws RepositoryException {
        int version = root.has("schemaVersion") ? root.get("schemaVersion").asInt(1) : 1;
        if (version > GameAnalysis.CURRENT_SCHEMA_VERSION) {
            throw new RepositoryException(RepositoryException.Kind.UNSUPPORTED_VERSION,
                    "This analysis was saved by a newer version of the application (format v" + version
                            + "). Please update the application to open it.");
        }
        if (version == GameAnalysis.CURRENT_SCHEMA_VERSION) {
            return new Result(false);
        }
        if (version < 2) {
            migrateV1toV2(root);
        }
        root.put("schemaVersion", GameAnalysis.CURRENT_SCHEMA_VERSION);
        return new Result(true);
    }

    private static void migrateV1toV2(ObjectNode root) {
        JsonNode moves = root.get("moves");
        if (moves == null || !moves.isArray()) {
            return;
        }
        for (JsonNode move : moves) {
            boolean whiteMoved = move.path("whiteToMove").asBoolean(true);
            // A mate-0 evaluation after a move means the mover just delivered checkmate.
            repairCheckmate(move.get("evalAfter"), whiteMoved);
        }
    }

    private static void repairCheckmate(JsonNode eval, boolean whiteDelivered) {
        if (eval instanceof ObjectNode obj && obj.hasNonNull("mateIn") && obj.get("mateIn").asInt() == 0
                && !obj.hasNonNull("whiteDeliveredMate")) {
            obj.put("whiteDeliveredMate", whiteDelivered);
        }
    }
}

package pl.lokos.tools.database;

import java.util.UUID;

public record PlayerSnapshot(
        UUID sessionId,
        UUID playerId,
        String name,
        long startedAt,
        long savedAt,
        long durationMs) {
}

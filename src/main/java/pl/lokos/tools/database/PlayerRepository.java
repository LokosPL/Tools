package pl.lokos.tools.database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class PlayerRepository {
    public record PlayerStats(String name, long joins, long playtimeMs) {}

    private final DatabaseExecutor database;
    private final SqlDialect dialect;

    public PlayerRepository(DatabaseExecutor database) {
        this.database = database;
        this.dialect = database.dialect();
    }

    public CompletableFuture<Void> recordJoin(UUID playerId, String name, long now) {
        return database.query(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(dialect.playerJoin())) {
                statement.setString(1, playerId.toString());
                statement.setString(2, name);
                statement.setLong(3, now);
                statement.setLong(4, now);
                statement.executeUpdate();
            }
            return null;
        });
    }

    public CompletableFuture<Void> saveSession(PlayerSnapshot snapshot) {
        return database.query(connection -> {
            try (PreparedStatement player = connection.prepareStatement(dialect.playerUpdate())) {
                player.setString(1, snapshot.playerId().toString());
                player.setString(2, snapshot.name());
                player.setLong(3, snapshot.startedAt());
                player.setLong(4, snapshot.savedAt());
                player.executeUpdate();
            }
            try (PreparedStatement session = connection.prepareStatement(dialect.sessionUpdate())) {
                session.setString(1, snapshot.sessionId().toString());
                session.setString(2, snapshot.playerId().toString());
                session.setLong(3, snapshot.startedAt());
                session.setLong(4, snapshot.durationMs());
                session.setLong(5, snapshot.savedAt());
                session.executeUpdate();
            }
            return null;
        });
    }

    public CompletableFuture<PlayerStats> findByName(String name) {
        return database.query(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT p.last_name, p.join_count, COALESCE(SUM(s.duration_ms), 0)
                    FROM tools_players p
                    LEFT JOIN tools_sessions s ON s.player_uuid = p.player_uuid
                    WHERE p.last_name = ?
                    GROUP BY p.player_uuid, p.last_name, p.join_count
                    LIMIT 1
                    """)) {
                statement.setString(1, name);
                try (ResultSet result = statement.executeQuery()) {
                    return result.next()
                            ? new PlayerStats(result.getString(1), result.getLong(2), result.getLong(3))
                            : null;
                }
            }
        });
    }

    public CompletableFuture<Long> ping() {
        return database.query(connection -> {
            long started = System.nanoTime();
            try (PreparedStatement statement = connection.prepareStatement("SELECT 1");
                 ResultSet result = statement.executeQuery()) {
                result.next();
            }
            return (System.nanoTime() - started) / 1_000_000L;
        });
    }
}

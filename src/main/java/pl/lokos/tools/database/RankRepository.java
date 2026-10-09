package pl.lokos.tools.database;

import pl.lokos.tools.manager.RankSnapshot;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Repoz ytorium rang: wszystkie wywolania sa asynchroniczne przez DatabaseManager.
 * Nazwy są walidowane przez komende i dodatkowo parametryzowane w JDBC.
 */
public final class RankRepository {
    private final DatabaseExecutor database;

    public RankRepository(DatabaseExecutor database) {
        this.database = database;
    }

    public CompletableFuture<RankSnapshot> load() {
        return database.query(c -> {
            Map<String, RankSnapshot.Rank> ranks = new HashMap<>();
            Map<String, Set<String>> permissions = new HashMap<>();
            Map<UUID, RankSnapshot.Grant> grants = new HashMap<>();
            Map<UUID, Boolean> restoredOps = new HashMap<>();
            try (PreparedStatement sql = c.prepareStatement("SELECT name, prefix, suffix, position, join_message FROM tools_ranks");
                 ResultSet rs = sql.executeQuery()) {
                while (rs.next()) {
                    int position = rs.getInt(4);
                    Integer priority = rs.wasNull() ? null : position;
                    ranks.put(rs.getString(1), new RankSnapshot.Rank(rs.getString(1),
                            rs.getString(2), rs.getString(3), priority, rs.getString(5)));
                }
            }
            try (PreparedStatement sql = c.prepareStatement("SELECT rank_name, permission FROM tools_rank_permissions");
                 ResultSet rs = sql.executeQuery()) {
                while (rs.next()) {
                    permissions.computeIfAbsent(rs.getString(1), unused -> new HashSet<>()).add(rs.getString(2));
                }
            }
            try (PreparedStatement sql = c.prepareStatement("SELECT player_uuid, rank_name, expires_at FROM tools_player_ranks");
                 ResultSet rs = sql.executeQuery()) {
                while (rs.next()) {
                    long expires = rs.getLong(3);
                    Long expiresAt = rs.wasNull() ? null : expires;
                    grants.put(UUID.fromString(rs.getString(1)), new RankSnapshot.Grant(
                            rs.getString(2), expiresAt));
                }
            }
            try (PreparedStatement sql = c.prepareStatement("SELECT player_uuid, original_op FROM tools_rank_op_restore");
                 ResultSet rs = sql.executeQuery()) {
                while (rs.next()) {
                    restoredOps.put(UUID.fromString(rs.getString(1)), rs.getBoolean(2));
                }
            }
            Map<String, Set<String>> frozen = new HashMap<>();
            permissions.forEach((key, value) -> frozen.put(key, Set.copyOf(value)));
            return new RankSnapshot(Map.copyOf(ranks), Map.copyOf(frozen), Map.copyOf(grants),
                    Map.copyOf(restoredOps));
        });
    }

    public CompletableFuture<Void> create(String name, String prefix, String suffix) {
        return change("INSERT INTO tools_ranks(name,prefix,suffix) VALUES(?,?,?)", name, prefix, suffix);
    }

    public CompletableFuture<Void> addPermission(String name, String permission) {
        return change("INSERT IGNORE INTO tools_rank_permissions(rank_name,permission) VALUES(?,?)", name, permission);
    }

    public CompletableFuture<Void> position(String name, int priority) {
        return changeExisting("UPDATE tools_ranks SET position=? WHERE name=?", priority, name);
    }

    public CompletableFuture<Void> joinMessage(String name, String message) {
        return changeExisting("UPDATE tools_ranks SET join_message=? WHERE name=?", message, name);
    }

    public CompletableFuture<Void> edit(String name, String field, String value) {
        String sql = switch (field) {
            case "prefix" -> "UPDATE tools_ranks SET prefix=? WHERE name=?";
            case "sufix" -> "UPDATE tools_ranks SET suffix=? WHERE name=?";
            case "nazwa" -> "UPDATE tools_ranks SET name=? WHERE name=?";
            default -> throw new IllegalArgumentException("Nieznane pole rangi.");
        };
        return changeExisting(sql, value, name);
    }

    public CompletableFuture<Void> delete(String name) {
        return changeExisting("DELETE FROM tools_ranks WHERE name=?", name);
    }

    /** Player data only: rank definitions stay in Ranks.json. */
    public CompletableFuture<Void> renameGrants(String previous,String next) {
        return change("UPDATE tools_player_ranks SET rank_name=? WHERE rank_name=?",next,previous);
    }
    public CompletableFuture<Void> deleteGrants(String name) {
        return change("DELETE FROM tools_player_ranks WHERE rank_name=?",name);
    }

    /** Zapis z odczytem kontrolnym w tej samej sesji JDBC. */
    public CompletableFuture<Void> grant(UUID uuid, String rank, Long expires) {
        return database.query(c -> {
            try (PreparedStatement stmt = c.prepareStatement(
                    "INSERT INTO tools_player_ranks(player_uuid,rank_name,expires_at) VALUES(?,?,?) "
                            + "ON DUPLICATE KEY UPDATE rank_name=VALUES(rank_name),expires_at=VALUES(expires_at)")) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, rank);
                if (expires == null) stmt.setNull(3, java.sql.Types.BIGINT);
                else stmt.setLong(3, expires);
                stmt.executeUpdate();
            }
            try (PreparedStatement statement = c.prepareStatement(
                    "SELECT rank_name,expires_at FROM tools_player_ranks WHERE player_uuid=?")) {
                statement.setString(1, uuid.toString());
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) throw new SQLException("Nie zapisano przypisania rangi.");
                    String actualRank = result.getString(1);
                    long actualTime = result.getLong(2);
                    Long actualExpiry = result.wasNull() ? null : actualTime;
                    if (!actualRank.equalsIgnoreCase(rank) || !Objects.equals(actualExpiry, expires))
                        throw new SQLException("Weryfikacja nadania nie powiodła się.");
                }
            }
            return null;
        });
    }

    public CompletableFuture<RankSnapshot.Grant> findGrant(UUID uuid) {
        return database.query(c -> {
            try (PreparedStatement statement = c.prepareStatement(
                    "SELECT rank_name, expires_at FROM tools_player_ranks WHERE player_uuid=?")) {
                statement.setString(1, uuid.toString());
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) return null;
                    String rankName = result.getString(1);
                    long time = result.getLong(2);
                    Long expiresAt = result.wasNull() ? null : time;
                    return new RankSnapshot.Grant(rankName, expiresAt);
                }
            }
        });
    }

    public CompletableFuture<Void> expire(long now) {
        return database.query(c -> {
            try (PreparedStatement stmt = c.prepareStatement("DELETE FROM tools_player_ranks WHERE expires_at IS NOT NULL AND expires_at <= ?")) {
                stmt.setLong(1, now);
                stmt.executeUpdate();
            }
            return null;
        });
    }

    public CompletableFuture<UUID> findPlayer(String name) {
        return database.query(c -> {
            try (PreparedStatement stmt = c.prepareStatement(
                    "SELECT player_uuid FROM tools_players WHERE last_name=? ORDER BY last_seen DESC LIMIT 1")) {
                stmt.setString(1, name);
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next() ? UUID.fromString(rs.getString(1)) : null;
                }
            }
        });
    }

    public CompletableFuture<Long> count(String name) {
        return database.query(c -> {
            try (PreparedStatement stmt = c.prepareStatement(
                    "SELECT COUNT(*) FROM tools_player_ranks WHERE rank_name=? AND (expires_at IS NULL OR expires_at > ?)")) {
                stmt.setString(1, name);
                stmt.setLong(2, System.currentTimeMillis());
                try (ResultSet rs = stmt.executeQuery()) {
                    rs.next();
                    return rs.getLong(1);
                }
            }
        });
    }

    public CompletableFuture<Void> rememberOp(UUID uuid, boolean original) {
        return change("INSERT IGNORE INTO tools_rank_op_restore(player_uuid,original_op) VALUES(?,?)",
                uuid.toString(), original);
    }

    public CompletableFuture<Void> forgetOp(UUID uuid) {
        return change("DELETE FROM tools_rank_op_restore WHERE player_uuid=?", uuid.toString());
    }

    private CompletableFuture<Void> change(String sql, Object... args) {
        return database.query(c -> {
            try (PreparedStatement stmt = c.prepareStatement(sql)) {
                for (int i = 0; i < args.length; i++) stmt.setObject(i + 1, args[i]);
                stmt.executeUpdate();
            }
            return null;
        });
    }

    private CompletableFuture<Void> changeExisting(String sql, Object... args) {
        return database.query(c -> {
            try (PreparedStatement stmt = c.prepareStatement(sql)) {
                for (int i = 0; i < args.length; i++) stmt.setObject(i + 1, args[i]);
                if (stmt.executeUpdate() < 1) throw new SQLException("Nie znaleziono rangi lub brak zmian.");
            }
            return null;
        });
    }
}

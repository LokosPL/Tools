package pl.lokos.tools.database;

/** Dialekt tylko dla operacji, które nie są przenośne w JDBC. */
public final class SqlDialect {
    private final DatabaseType type;
    public SqlDialect(DatabaseType type) { this.type = type; }
    public DatabaseType type() { return type; }
    public boolean sqlite() { return type.sqlite(); }

    public String playerJoin() {
        return sqlite() ?
            "INSERT INTO tools_players(player_uuid,last_name,first_seen,last_seen,join_count) VALUES(?,?,?,?,1) "
            + "ON CONFLICT(player_uuid) DO UPDATE SET last_name=excluded.last_name,"
            + "last_seen=MAX(last_seen,excluded.last_seen),join_count=join_count+1" :
            "INSERT INTO tools_players(player_uuid,last_name,first_seen,last_seen,join_count) VALUES(?,?,?,?,1) "
            + "ON DUPLICATE KEY UPDATE last_name=VALUES(last_name),"
            + "last_seen=GREATEST(last_seen,VALUES(last_seen)),join_count=join_count+1";
    }

    public String playerUpdate() {
        return sqlite() ?
            "INSERT INTO tools_players(player_uuid,last_name,first_seen,last_seen,join_count) VALUES(?,?,?,?,0) "
            + "ON CONFLICT(player_uuid) DO UPDATE SET last_name=excluded.last_name,"
            + "last_seen=MAX(last_seen,excluded.last_seen)" :
            "INSERT INTO tools_players(player_uuid,last_name,first_seen,last_seen,join_count) VALUES(?,?,?,?,0) "
            + "ON DUPLICATE KEY UPDATE last_name=VALUES(last_name),"
            + "last_seen=GREATEST(last_seen,VALUES(last_seen))";
    }

    public String sessionUpdate() {
        return sqlite() ?
            "INSERT INTO tools_sessions(session_uuid,player_uuid,started_at,duration_ms,updated_at) VALUES(?,?,?,?,?) "
            + "ON CONFLICT(session_uuid) DO UPDATE SET duration_ms=MAX(duration_ms,excluded.duration_ms),"
            + "updated_at=MAX(updated_at,excluded.updated_at)" :
            "INSERT INTO tools_sessions(session_uuid,player_uuid,started_at,duration_ms,updated_at) VALUES(?,?,?,?,?) "
            + "ON DUPLICATE KEY UPDATE duration_ms=GREATEST(duration_ms,VALUES(duration_ms)),"
            + "updated_at=GREATEST(updated_at,VALUES(updated_at))";
    }

    public String grant() {
        return sqlite() ?
            "INSERT INTO tools_player_ranks(player_uuid,rank_name,expires_at) VALUES(?,?,?) "
            + "ON CONFLICT(player_uuid) DO UPDATE SET rank_name=excluded.rank_name,expires_at=excluded.expires_at" :
            "INSERT INTO tools_player_ranks(player_uuid,rank_name,expires_at) VALUES(?,?,?) "
            + "ON DUPLICATE KEY UPDATE rank_name=VALUES(rank_name),expires_at=VALUES(expires_at)";
    }
    public String rememberOp() {
        return sqlite() ?
            "INSERT OR IGNORE INTO tools_rank_op_restore(player_uuid,original_op) VALUES(?,?)" :
            "INSERT IGNORE INTO tools_rank_op_restore(player_uuid,original_op) VALUES(?,?)";
    }
    public String addPermission() {
        return sqlite() ?
            "INSERT OR IGNORE INTO tools_rank_permissions(rank_name,permission) VALUES(?,?)" :
            "INSERT IGNORE INTO tools_rank_permissions(rank_name,permission) VALUES(?,?)";
    }
    public String regionFlag() {
        return sqlite() ?
            "INSERT INTO tools_region_flags(region_name,flag_name,allowed) VALUES(?,?,?) "
            + "ON CONFLICT(region_name,flag_name) DO UPDATE SET allowed=excluded.allowed" :
            "INSERT INTO tools_region_flags(region_name,flag_name,allowed) VALUES(?,?,?) "
            + "ON DUPLICATE KEY UPDATE allowed=VALUES(allowed)";
    }
    public String regionSetting() {
        return sqlite() ?
            "INSERT INTO tools_region_settings(config_key,config_value) VALUES('spawn',?) "
            + "ON CONFLICT(config_key) DO UPDATE SET config_value=excluded.config_value" :
            "INSERT INTO tools_region_settings(config_key,config_value) VALUES('spawn',?) "
            + "ON DUPLICATE KEY UPDATE config_value=VALUES(config_value)";
    }
}

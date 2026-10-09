-- SQLite WAL: per-connection PRAGMA foreign_keys=ON in Hikari.
CREATE TABLE IF NOT EXISTS tools_players (
 player_uuid TEXT PRIMARY KEY, last_name TEXT NOT NULL, first_seen INTEGER NOT NULL,
 last_seen INTEGER NOT NULL, join_count INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_tools_player_name ON tools_players(last_name);
CREATE TABLE IF NOT EXISTS tools_sessions (
 session_uuid TEXT PRIMARY KEY, player_uuid TEXT NOT NULL,
 started_at INTEGER NOT NULL, duration_ms INTEGER NOT NULL DEFAULT 0, updated_at INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_tools_session_player ON tools_sessions(player_uuid);
CREATE TABLE IF NOT EXISTS tools_ranks (
 name TEXT PRIMARY KEY, prefix TEXT NOT NULL DEFAULT '', suffix TEXT NOT NULL DEFAULT '',
 position INTEGER, join_message TEXT NOT NULL DEFAULT ''
);
CREATE TABLE IF NOT EXISTS tools_rank_permissions (
 rank_name TEXT NOT NULL, permission TEXT NOT NULL, PRIMARY KEY(rank_name,permission)
);
CREATE TABLE IF NOT EXISTS tools_player_ranks (
 player_uuid TEXT PRIMARY KEY, rank_name TEXT NOT NULL, expires_at INTEGER
);
CREATE INDEX IF NOT EXISTS idx_rank_expiry ON tools_player_ranks(expires_at);
CREATE TABLE IF NOT EXISTS tools_regions (
 name TEXT PRIMARY KEY, world_uuid TEXT NOT NULL,
 min_x INTEGER NOT NULL, max_x INTEGER NOT NULL, min_z INTEGER NOT NULL, max_z INTEGER NOT NULL,
 parent TEXT, entry_rank TEXT, spawn_x REAL, spawn_y REAL, spawn_z REAL, spawn_yaw REAL, spawn_pitch REAL,
 FOREIGN KEY(parent) REFERENCES tools_regions(name) ON DELETE CASCADE ON UPDATE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_regions_world ON tools_regions(world_uuid);
CREATE TABLE IF NOT EXISTS tools_region_flags (
 region_name TEXT NOT NULL, flag_name TEXT NOT NULL, allowed INTEGER NOT NULL,
 PRIMARY KEY(region_name,flag_name),
 FOREIGN KEY(region_name) REFERENCES tools_regions(name) ON DELETE CASCADE ON UPDATE CASCADE
);
CREATE TABLE IF NOT EXISTS tools_region_settings (
 config_key TEXT PRIMARY KEY, config_value TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS tools_rank_op_restore (
 player_uuid TEXT PRIMARY KEY, original_op INTEGER NOT NULL
);

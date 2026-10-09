-- Baza MySQL / MariaDB. Na istniejącej instalacji Flyway używa wersji bazowej 1
-- bez ponownego zakładania lub usuwania obecnych tabel.
CREATE TABLE IF NOT EXISTS tools_players (
 player_uuid CHAR(36) NOT NULL PRIMARY KEY, last_name VARCHAR(16) NOT NULL,
 first_seen BIGINT NOT NULL, last_seen BIGINT NOT NULL,
 join_count BIGINT NOT NULL DEFAULT 0, KEY idx_tools_player_name(last_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS tools_sessions (
 session_uuid CHAR(36) NOT NULL PRIMARY KEY, player_uuid CHAR(36) NOT NULL,
 started_at BIGINT NOT NULL, duration_ms BIGINT NOT NULL DEFAULT 0,
 updated_at BIGINT NOT NULL, KEY idx_tools_session_player(player_uuid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS tools_ranks (
 name VARCHAR(24) NOT NULL PRIMARY KEY, prefix VARCHAR(256) NOT NULL DEFAULT '',
 suffix VARCHAR(256) NOT NULL DEFAULT '', position SMALLINT NULL,
 join_message VARCHAR(512) NOT NULL DEFAULT ''
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS tools_rank_permissions (
 rank_name VARCHAR(24) NOT NULL, permission VARCHAR(128) NOT NULL,
 PRIMARY KEY(rank_name,permission)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS tools_player_ranks (
 player_uuid CHAR(36) NOT NULL PRIMARY KEY, rank_name VARCHAR(24) NOT NULL,
 expires_at BIGINT NULL, KEY idx_rank_expiry(expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS tools_regions (
 name VARCHAR(24) NOT NULL PRIMARY KEY, world_uuid CHAR(36) NOT NULL,
 min_x INT NOT NULL, max_x INT NOT NULL, min_z INT NOT NULL, max_z INT NOT NULL,
 parent VARCHAR(24) NULL, entry_rank VARCHAR(24) NULL,
 spawn_x DOUBLE NULL, spawn_y DOUBLE NULL, spawn_z DOUBLE NULL,
 spawn_yaw FLOAT NULL, spawn_pitch FLOAT NULL, KEY idx_regions_world(world_uuid),
 FOREIGN KEY(parent) REFERENCES tools_regions(name) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS tools_region_flags (
 region_name VARCHAR(24) NOT NULL, flag_name VARCHAR(32) NOT NULL,
 allowed BOOLEAN NOT NULL, PRIMARY KEY(region_name,flag_name),
 FOREIGN KEY(region_name) REFERENCES tools_regions(name) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS tools_region_settings (
 config_key VARCHAR(32) PRIMARY KEY, config_value VARCHAR(64) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS tools_rank_op_restore (
 player_uuid CHAR(36) NOT NULL PRIMARY KEY, original_op BOOLEAN NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

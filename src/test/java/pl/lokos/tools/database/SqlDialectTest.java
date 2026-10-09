package pl.lokos.tools.database;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SqlDialectTest {
    @Test void usesExplicitConflictTargetsOnSqlite() {
        SqlDialect sql = new SqlDialect(DatabaseType.SQLITE);
        assertTrue(sql.playerJoin().contains("ON CONFLICT(player_uuid)"));
        assertTrue(sql.grant().contains("ON CONFLICT(player_uuid)"));
        assertTrue(sql.regionFlag().contains("ON CONFLICT(region_name,flag_name)"));
        assertTrue(sql.rememberOp().contains("INSERT OR IGNORE"));
    }
    @Test void usesMysqlUpsertForMariaDb() {
        SqlDialect sql = new SqlDialect(DatabaseType.MARIADB);
        assertTrue(sql.playerUpdate().contains("ON DUPLICATE KEY UPDATE"));
    }
    @Test void backendValidation() {
        assertEquals(DatabaseType.SQLITE, DatabaseType.parse("sqlite"));
        assertEquals(DatabaseType.MARIADB, DatabaseType.parse("MariaDb"));
        assertThrows(IllegalArgumentException.class, () -> DatabaseType.parse("redis"));
    }
}

package pl.lokos.tools.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.lokos.tools.region.RegionFlag;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class SqliteBackendIntegrationTest {
    @TempDir Path temp;

    @Test
    void migratesAndHandlesPlayersRanksAndRegionsOnRealSqlite() throws Exception {
        Path file = temp.resolve("integration.db");
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + file);
        config.setDriverClassName("org.sqlite.JDBC");
        config.setConnectionInitSql("PRAGMA foreign_keys=ON");
        config.setMaximumPoolSize(1);

        try (HikariDataSource pool = new HikariDataSource(config)) {
            Flyway flyway = Flyway.configure()
                    .dataSource(pool)
                    .locations("classpath:db/migration/sqlite")
                    .load();
            assertEquals(2, flyway.migrate().migrationsExecuted);
            assertEquals(0, flyway.migrate().migrationsExecuted);

            DatabaseExecutor executor = new DatabaseExecutor() {
                @Override public SqlDialect dialect() {
                    return new SqlDialect(DatabaseType.SQLITE);
                }
                @Override public <T> CompletableFuture<T> query(DatabaseManager.SqlOperation<T> op) {
                    try (Connection connection = pool.getConnection()) {
                        return CompletableFuture.completedFuture(op.run(connection));
                    } catch (Exception error) {
                        return CompletableFuture.failedFuture(error);
                    }
                }
            };
            PlayerRepository players = new PlayerRepository(executor);
            UUID uuid = UUID.randomUUID();
            players.recordJoin(uuid, "Tester", 10L).join();
            players.recordJoin(uuid, "Tester", 20L).join();
            UUID session = UUID.randomUUID();
            PlayerSnapshot saved = new PlayerSnapshot(session, uuid, "Tester", 10, 50, 40);
            players.saveSession(saved).join();
            players.saveSession(saved).join();
            assertEquals(2L, players.findByName("Tester").join().joins());
            assertEquals(40L, players.findByName("Tester").join().playtimeMs());

            RankRepository ranks = new RankRepository(executor);
            ranks.create("test", "&aTEST", "").join();
            ranks.addPermission("test", "tools.region.admin").join();
            ranks.grant(uuid, "test", null).join();
            assertNull(ranks.findGrant(uuid).join().expiresAt());
            assertEquals("test", ranks.findGrant(uuid).join().rank());
            ranks.rememberOp(uuid, false).join();
            ranks.rememberOp(uuid, true).join();
            assertEquals(false, ranks.loadAssignments().join().opRestores().get(uuid));

            try (Connection conn = pool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "INSERT INTO tools_regions(name,world_uuid,min_x,max_x,min_z,max_z) VALUES(?,?,?,?,?,?)")) {
                stmt.setString(1, "centrum");
                stmt.setString(2, UUID.randomUUID().toString());
                stmt.setInt(3, 0);stmt.setInt(4, 100);stmt.setInt(5, 0);stmt.setInt(6, 100);
                stmt.executeUpdate();
            }
            RegionRepository regions = new RegionRepository(executor);
            regions.flag("centrum", RegionFlag.PVP, true).join();
            regions.flag("centrum", RegionFlag.PVP, false).join();
            try (Connection conn = pool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "SELECT allowed FROM tools_region_flags WHERE region_name='centrum'");
                 ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                assertFalse(rs.getBoolean(1));
            }
        }
    }
}

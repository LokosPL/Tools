package pl.lokos.tools.database;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Uruchamia właściwy proces Flyway na usłudze MariaDB w CI.
 * Działa także, gdy inne testy zdążyły wcześniej utworzyć dotychczasowe tabele.
 */
class FlywayMariaDbMigrationTest {
    @Test
    void migratesLegacySchemaAndIsIdempotent() throws Exception {
        String url = System.getenv("TOOLS_IT_DB_URL");
        Assumptions.assumeTrue(url != null && !url.isBlank());
        String user = System.getenv().getOrDefault("TOOLS_IT_DB_USER", "root");
        String password = System.getenv().getOrDefault("TOOLS_IT_DB_PASS", "");

        Flyway flyway = Flyway.configure()
                .dataSource(url, user, password)
                .locations("classpath:db/migration/mysql")
                .baselineOnMigrate(true)
                .baselineVersion("1")
                .validateMigrationNaming(true)
                .load();

        assertTrue(flyway.migrate().success);
        assertEquals(0, flyway.migrate().migrationsExecuted);
        try (Connection connection = DriverManager.getConnection(url, user, password);
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM tools_audit_events")) {
            assertTrue(result.next());
            assertEquals(0, result.getInt(1));
        }
    }
}

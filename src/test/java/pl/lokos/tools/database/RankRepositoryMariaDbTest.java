package pl.lokos.tools.database;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import pl.lokos.tools.manager.RankSnapshot;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.UUID;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/** Integracyjny test prawdziwej bazy MariaDB podczas kompilacji GitHub Actions. */
class RankRepositoryMariaDbTest {
    @Test
    void createsSetsPositionGrantsVerifiesExpiresAndDeletes() throws Exception {
        String url = System.getenv("TOOLS_IT_DB_URL");
        Assumptions.assumeTrue(url != null && !url.isBlank(),
                "Test integracyjny wymaga usługi MariaDB.");
        String user = System.getenv().getOrDefault("TOOLS_IT_DB_USER", "root");
        String pass = System.getenv().getOrDefault("TOOLS_IT_DB_PASS", "");

        try (Connection connection = DriverManager.getConnection(url, user, pass)) {
            DatabaseManager.createSchema(connection);
        }

        DatabaseExecutor executor = new DatabaseExecutor() {
            @Override
            public <T> CompletableFuture<T> query(DatabaseManager.SqlOperation<T> operation) {
                try (Connection connection = DriverManager.getConnection(url, user, pass)) {
                    return CompletableFuture.completedFuture(operation.run(connection));
                } catch (Exception error) {
                    return CompletableFuture.failedFuture(error);
                }
            }
        };
        RankRepository repository = new RankRepository(executor);
        UUID player = UUID.randomUUID();
        String rank = "test" + UUID.randomUUID().toString().substring(0, 8);
        try {
            repository.create(rank, "&eTEST_ ", "&7").join();
            repository.position(rank, 1).join();
            repository.addPermission(rank, "tools.test").join();

            // Gwiazdka = ranga nadana na zawsze, czyli SQL NULL.
            repository.grant(player, rank, null).join();
            RankSnapshot.Grant permanent = repository.findGrant(player).join();
            assertNotNull(permanent);
            assertEquals(rank, permanent.rank());
            assertNull(permanent.expiresAt());

            RankSnapshot state = repository.load().join();
            assertEquals(rank, state.forPlayer(player).name());
            assertEquals(Set.of("tools.test"), state.permissionsFor(player));
            assertEquals(1L, repository.count(rank).join());

            long expiry = System.currentTimeMillis() + 60_000;
            repository.grant(player, rank, expiry).join();
            assertEquals(expiry, repository.findGrant(player).join().expiresAt());

            repository.expire(expiry + 1).join();
            assertNull(repository.findGrant(player).join());
            assertEquals(0L, repository.count(rank).join());

            // JSON-only rank: no record in tools_ranks. FK must not block grants.
            String jsonOnly="json"+UUID.randomUUID().toString().substring(0,8);
            repository.grant(player,jsonOnly,null).join();
            assertEquals(jsonOnly,repository.loadAssignments().join().grants().get(player).rank());
            repository.deleteGrants(jsonOnly).join();
        } finally {
            repository.delete(rank).join();
        }
    }
}

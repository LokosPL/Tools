package pl.lokos.tools.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.java.JavaPlugin;
import pl.lokos.tools.config.ToolsConfig;
import pl.lokos.tools.enums.DatabaseStatus;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public final class DatabaseManager {
    @FunctionalInterface
    public interface SqlOperation<T> {
        T run(Connection connection) throws SQLException;
    }

    private final JavaPlugin plugin;
    private volatile HikariDataSource source;
    private final ThreadPoolExecutor executor;
    private final AtomicReference<DatabaseStatus> status = new AtomicReference<>(DatabaseStatus.STARTING);
    private final CompletableFuture<Void> ready = new CompletableFuture<>();

    public DatabaseManager(JavaPlugin plugin, ToolsConfig.Database config) {
        this.plugin = plugin;
        this.executor = new ThreadPoolExecutor(
                Math.min(2, config.poolSize()), Math.min(2, config.poolSize()),
                0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(4096),
                task -> {
                    Thread worker = new Thread(task, "Tools-MySQL");
                    worker.setDaemon(true);
                    return worker;
                }, new ThreadPoolExecutor.AbortPolicy());

        // Wszystkie operacje sieciowe i SQL na pracowniku, poza tickiem Paper.
        executor.execute(() -> initialize(config));
    }

    private void initialize(ToolsConfig.Database config) {
        try {
            if (config.createDatabaseIfMissing()) {
                createDatabase(config);
            }

            HikariDataSource pool = new HikariDataSource(newHikariConfig(config));
            source = pool;
            try (Connection connection = pool.getConnection()) {
                createSchema(connection);
            }

            if (status.compareAndSet(DatabaseStatus.STARTING, DatabaseStatus.READY)) {
                ready.complete(null);
                plugin.getLogger().info("MySQL polaczony (" + config.host() + ":" + config.port()
                        + ", baza " + config.database() + "), tabele gotowe.");
            } else {
                ready.completeExceptionally(new IllegalStateException("Plugin jest zamykany."));
            }
        } catch (Throwable error) {
            status.compareAndSet(DatabaseStatus.STARTING, DatabaseStatus.FAILED);
            ready.completeExceptionally(error);
            plugin.getLogger().severe("Nie udalo sie uruchomic MySQL "
                    + config.host() + ":" + config.port() + " / " + config.database()
                    + ": " + error.getMessage());
        } finally {
            if (status.get() != DatabaseStatus.READY) {
                HikariDataSource pool = source;
                if (pool != null) {
                    pool.close();
                    source = null;
                }
            }
        }
    }

    private static void createDatabase(ToolsConfig.Database config) throws SQLException {
        // ToolsConfig validates database name to [a-zA-Z0-9_]+.
        String url = "jdbc:mysql://" + config.host() + ":" + config.port() + "/"
                + "?sslMode=" + config.sslMode()
                + "&connectTimeout=" + config.connectionTimeoutMs()
                + "&socketTimeout=10000";
        try (Connection connection = java.sql.DriverManager.getConnection(
                url, config.username(), config.resolvedPassword());
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + config.database()
                    + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        }
    }

    private static HikariConfig newHikariConfig(ToolsConfig.Database config) {
        HikariConfig hikari = new HikariConfig();
        hikari.setPoolName("Tools-MySQL");
        hikari.setJdbcUrl("jdbc:mysql://" + config.host() + ":" + config.port() + "/" + config.database());
        hikari.setUsername(config.username());
        hikari.setPassword(config.resolvedPassword());
        hikari.setMaximumPoolSize(config.poolSize());
        hikari.setMinimumIdle(1);
        hikari.setConnectionTimeout(config.connectionTimeoutMs());
        hikari.setValidationTimeout(Math.min(3000, config.connectionTimeoutMs()));
        hikari.setIdleTimeout(600_000);
        hikari.setMaxLifetime(1_800_000);
        hikari.setKeepaliveTime(120_000);
        hikari.setInitializationFailTimeout(-1);
        hikari.addDataSourceProperty("sslMode", config.sslMode());
        hikari.addDataSourceProperty("connectionTimeZone", "UTC");
        hikari.addDataSourceProperty("cachePrepStmts", "true");
        hikari.addDataSourceProperty("prepStmtCacheSize", "250");
        hikari.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikari.addDataSourceProperty("useServerPrepStmts", "true");
        hikari.addDataSourceProperty("tcpKeepAlive", "true");
        hikari.addDataSourceProperty("connectTimeout", Integer.toString(config.connectionTimeoutMs()));
        hikari.addDataSourceProperty("socketTimeout", "10000");
        return hikari;
    }

    private static void createSchema(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS tools_players (
                      player_uuid CHAR(36) NOT NULL PRIMARY KEY,
                      last_name VARCHAR(16) NOT NULL,
                      first_seen BIGINT NOT NULL,
                      last_seen BIGINT NOT NULL,
                      join_count BIGINT NOT NULL DEFAULT 0,
                      KEY idx_tools_player_name (last_name)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS tools_sessions (
                      session_uuid CHAR(36) NOT NULL PRIMARY KEY,
                      player_uuid CHAR(36) NOT NULL,
                      started_at BIGINT NOT NULL,
                      duration_ms BIGINT NOT NULL DEFAULT 0,
                      updated_at BIGINT NOT NULL,
                      KEY idx_tools_session_player (player_uuid)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                    """);
        }
    }

    public DatabaseStatus status() {
        return status.get();
    }

    public <T> CompletableFuture<T> query(SqlOperation<T> operation) {
        return ready.thenCompose(unused -> runRaw(operation));
    }

    private <T> CompletableFuture<T> runRaw(SqlOperation<T> operation) {
        CompletableFuture<T> future = new CompletableFuture<>();
        try {
            executor.execute(() -> {
                HikariDataSource activeSource = source;
                if (activeSource == null) {
                    future.completeExceptionally(new IllegalStateException("Pula MySQL nie jest dostepna."));
                    return;
                }
                try (Connection connection = activeSource.getConnection()) {
                    future.complete(operation.run(connection));
                } catch (Throwable error) {
                    future.completeExceptionally(error);
                }
            });
        } catch (RejectedExecutionException error) {
            future.completeExceptionally(error);
        }
        return future;
    }

    public void shutdown(CompletableFuture<?> pending) {
        status.set(DatabaseStatus.CLOSING);
        try {
            pending.get(8, TimeUnit.SECONDS);
        } catch (Exception error) {
            plugin.getLogger().warning("Nie wszystkie ostatnie zapisy SQL zostaly potwierdzone: " + error.getMessage());
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
        HikariDataSource activeSource = source;
        if (activeSource != null) {
            activeSource.close();
            source = null;
        }
        status.set(DatabaseStatus.CLOSED);
    }
}

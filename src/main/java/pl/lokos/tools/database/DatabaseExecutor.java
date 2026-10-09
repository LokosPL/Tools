package pl.lokos.tools.database;

import java.util.concurrent.CompletableFuture;

/** Kontrakt asynchronicznych operacji SQL, niezależny od konkretnej puli JDBC. */
public interface DatabaseExecutor {
    <T> CompletableFuture<T> query(DatabaseManager.SqlOperation<T> operation);
}

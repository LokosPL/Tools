package pl.lokos.tools.diagnostics;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.lokos.tools.database.DatabaseManager;

import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 * Diagnostyka niskokosztowa: bez skanowania heap, bez zapytań SQL na ticku.
 * Czasy modułów to czasy własnych zadań Tools, nie pełny profiler Paper.
 */
public final class MonitoringService {
    private static final Logger LOG = LoggerFactory.getLogger("Tools.monitoring");
    private final JavaPlugin plugin;
    private final DatabaseManager database;
    private final Map<String, Timings> modules = new ConcurrentHashMap<>();
    private volatile boolean running = true;
    private volatile double warnTps = 17.0;
    private volatile int warnHeapPercent = 85;
    private volatile long lastWarning;

    private static final class Timings {
        final LongAdder calls = new LongAdder();
        final LongAdder nanos = new LongAdder();
        final LongAdder slowCalls = new LongAdder();
        volatile long worstMs;
        void track(long elapsedNanos) {
            calls.increment();
            nanos.add(elapsedNanos);
            long ms = TimeUnit.NANOSECONDS.toMillis(elapsedNanos);
            if (ms > worstMs) worstMs = ms;
            if (ms >= 50) slowCalls.increment();
        }
    }
    public record ModuleTiming(String name, long calls, double meanMs, long maxMs, long slowCalls) {}
    public record Health(String databaseStatus, String backend, double tps,
                         long usedMb, long maxMb, int heapPercent, int threads,
                         int pluginTasks, long queries, long sqlFailures,
                         int sqlQueued, int sqlWaiting, long lastSqlMs,
                         List<ModuleTiming> modules) {}

    public MonitoringService(JavaPlugin plugin, DatabaseManager database) {
        this.plugin = Objects.requireNonNull(plugin);
        this.database = database;
    }

    public void configure(double minTps, int maxHeapPercent) {
        if (minTps < 5 || minTps > 20) throw new IllegalArgumentException("Minimalne TPS musi wynosić 5–20.");
        if (maxHeapPercent < 50 || maxHeapPercent > 99)
            throw new IllegalArgumentException("Próg pamięci musi wynosić 50–99%.");
        this.warnTps = minTps;
        this.warnHeapPercent = maxHeapPercent;
    }

    public Runnable measured(String module, Runnable task) {
        Objects.requireNonNull(module);
        Objects.requireNonNull(task);
        return () -> {
            long started = System.nanoTime();
            try { task.run(); }
            finally {
                if (running) {
                    modules.computeIfAbsent(module, key -> new Timings())
                            .track(System.nanoTime() - started);
                }
            }
        };
    }

    public Health snapshot() {
        // Wywoływane tylko z głównego wątku podczas komendy lub okresowego monitoringu.
        double[] tps = Bukkit.getTPS();
        double currentTps = tps.length == 0 ? 20 : Math.max(0, Math.min(20, tps[0]));
        Runtime runtime = Runtime.getRuntime();
        long used = runtime.totalMemory() - runtime.freeMemory();
        long max = runtime.maxMemory();
        int heap = max == 0 ? 0 : (int) Math.min(100, used * 100 / max);
        int tasks = (int) Bukkit.getScheduler().getPendingTasks().stream()
                .filter(task -> task.getOwner().equals(plugin)).count();
        DatabaseManager.Metrics db = database == null ? null : database.metrics();
        List<ModuleTiming> timings = new ArrayList<>();
        modules.forEach((name, time) -> {
            long count = time.calls.sum();
            if (count > 0) timings.add(new ModuleTiming(name, count,
                    time.nanos.sum() / 1_000_000.0 / count, time.worstMs, time.slowCalls.sum()));
        });
        timings.sort(Comparator.comparingDouble(ModuleTiming::meanMs).reversed());
        return new Health(
                db == null ? "Wyłączona" : db.status().displayName(),
                db == null ? "brak" : db.backend().name(),
                currentTps, used / 1_048_576, max / 1_048_576, heap,
                ManagementFactory.getThreadMXBean().getThreadCount(), tasks,
                db == null ? 0 : db.completed(), db == null ? 0 : db.failures(),
                db == null ? 0 : db.queued(), db == null ? 0 : db.waiting(),
                db == null ? 0 : db.lastQueryMs(), List.copyOf(timings));
    }

    /** Ostrzega raz na minutę, nie przypisuje spadku TPS bez dowodów. */
    public void check() {
        if (!running) return;
        Health health = snapshot();
        if (health.tps() >= warnTps && health.heapPercent() < warnHeapPercent && health.sqlQueued() < 1000) return;
        long now = System.currentTimeMillis();
        if (now - lastWarning < 60_000) return;
        lastWarning = now;
        LOG.warn("Stan serwera: TPS={} heap={}%, kolejka SQL={}, oczekujące SQL={}. "
                        + "To są sygnały diagnostyczne, nie dowód winy konkretnego modułu.",
                health.tps(), health.heapPercent(), health.sqlQueued(), health.sqlWaiting());
    }

    public void stop() { running = false; }
}

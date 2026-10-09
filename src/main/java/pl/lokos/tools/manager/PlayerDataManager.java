package pl.lokos.tools.manager;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import pl.lokos.tools.database.PlayerRepository;
import pl.lokos.tools.database.PlayerSnapshot;
import pl.lokos.tools.utils.ThreadChecks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public final class PlayerDataManager {
    private final JavaPlugin plugin;
    private final PlayerRepository repository;
    private final Map<UUID, Session> sessions = new HashMap<>();
    private final Map<UUID, UUID> onlineSessions = new HashMap<>();
    private boolean closed;

    public PlayerDataManager(JavaPlugin plugin, PlayerRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
    }

    public void playerJoined(Player player) {
        ThreadChecks.requirePrimaryThread();
        if (closed) {
            return;
        }
        long now = System.currentTimeMillis();
        Session session = new Session(UUID.randomUUID(), player.getUniqueId(), player.getName(), now);
        sessions.put(session.sessionId, session);
        onlineSessions.put(session.playerId, session.sessionId);
        repository.recordJoin(session.playerId, session.name, now).whenComplete((result, error) -> {
            if (error != null) {
                plugin.getLogger().log(Level.WARNING, "Nie udalo sie zapisac dolaczenia gracza " + session.name, error);
            }
        });
    }

    public void playerQuit(Player player) {
        ThreadChecks.requirePrimaryThread();
        UUID sessionId = onlineSessions.remove(player.getUniqueId());
        if (sessionId == null) {
            return;
        }
        Session session = sessions.get(sessionId);
        if (session != null) {
            session.captureTime();
            session.online = false;
            flush(session);
        }
    }

    public void autosave() {
        ThreadChecks.requirePrimaryThread();
        if (closed) {
            return;
        }
        for (Session session : new ArrayList<>(sessions.values())) {
            if (session.online) {
                session.captureTime();
            }
            flush(session);
        }
    }

    public int onlineCount() {
        return onlineSessions.size();
    }

    private void flush(Session session) {
        if (session.writing || session.elapsedMillis() <= session.persistedMillis) {
            cleanupIfFinished(session);
            return;
        }
        session.writing = true;
        long snapshotMillis = session.elapsedMillis();
        PlayerSnapshot snapshot = session.snapshot(snapshotMillis);
        repository.saveSession(snapshot).whenComplete((ignored, error) -> {
            if (!plugin.isEnabled() || closed) {
                return;
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                session.writing = false;
                if (error != null) {
                    plugin.getLogger().log(Level.WARNING,
                            "Blad zapisu sesji gracza " + session.name + " (ponowienie przy kolejnym autosave)", error);
                    return;
                }
                session.persistedMillis = Math.max(session.persistedMillis, snapshotMillis);
                if (session.elapsedMillis() > session.persistedMillis) {
                    flush(session);
                } else {
                    cleanupIfFinished(session);
                }
            });
        });
    }

    private void cleanupIfFinished(Session session) {
        if (!session.online && !session.writing && session.elapsedMillis() <= session.persistedMillis) {
            sessions.remove(session.sessionId);
        }
    }

    public CompletableFuture<Void> shutdownAndFlush() {
        ThreadChecks.requirePrimaryThread();
        closed = true;
        ArrayList<CompletableFuture<Void>> pending = new ArrayList<>();
        for (Session session : sessions.values()) {
            if (session.online) {
                session.captureTime();
            }
            // Ponowne zapisanie tej samej sesji jest bezpieczne:
            // SQL GREATEST nigdy nie doda dwa razy tego samego czasu.
            pending.add(repository.saveSession(session.snapshot(session.elapsedMillis())));
        }
        sessions.clear();
        onlineSessions.clear();
        return CompletableFuture.allOf(pending.toArray(new CompletableFuture[0]));
    }

    private static final class Session {
        private final UUID sessionId;
        private final UUID playerId;
        private final String name;
        private final long startedAt;
        private long lastCaptureNanos = System.nanoTime();
        private long elapsedNanos;
        private long persistedMillis = -1;
        private boolean writing;
        private boolean online = true;

        private Session(UUID sessionId, UUID playerId, String name, long startedAt) {
            this.sessionId = sessionId;
            this.playerId = playerId;
            this.name = name;
            this.startedAt = startedAt;
        }

        private void captureTime() {
            long now = System.nanoTime();
            elapsedNanos += Math.max(0L, now - lastCaptureNanos);
            lastCaptureNanos = now;
        }

        private long elapsedMillis() {
            return elapsedNanos / 1_000_000L;
        }

        private PlayerSnapshot snapshot(long durationMs) {
            return new PlayerSnapshot(sessionId, playerId, name, startedAt,
                    System.currentTimeMillis(), durationMs);
        }
    }
}

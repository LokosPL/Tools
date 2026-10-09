package pl.lokos.tools.manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.java.JavaPlugin;
import pl.lokos.tools.database.RankRepository;
import pl.lokos.tools.utils.ThreadChecks;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * Cache immutable rang jest bezpieczny takze dla AsyncChatEvent.
 * Wysylka SQL jest asynchroniczna; Bukkit API tylko na watku serwera.
 */
public final class RankManager {
    private final JavaPlugin plugin;
    private final RankRepository repository;
    private final Map<UUID, PermissionAttachment> attachments = new HashMap<>();
    private final Set<UUID> opPending = new HashSet<>();
    private volatile RankSnapshot snapshot = RankSnapshot.empty();
    private boolean stopping;
    private boolean expirePending;
    private RankVisualManager visuals;
    private CompletableFuture<Void> mutationTail = CompletableFuture.completedFuture(null);

    public RankManager(JavaPlugin plugin, RankRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
    }

    public void setVisuals(RankVisualManager visuals) {
        this.visuals = visuals;
    }

    public RankSnapshot snapshot() {
        return snapshot;
    }

    public RankRepository repository() {
        return repository;
    }

    public void start() {
        refresh().exceptionally(error -> {
            plugin.getLogger().log(Level.SEVERE, "Nie udalo sie pobrac rang z MySQL.", error);
            return null;
        });
    }

    public CompletableFuture<Void> refresh() {
        return repository.load().thenCompose(this::installSnapshot);
    }

    private CompletableFuture<Void> installSnapshot(RankSnapshot next) {
        CompletableFuture<Void> result = new CompletableFuture<>();
        if (stopping || !plugin.isEnabled()) {
            result.complete(null);
            return result;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            snapshot = next;
            for (Player player : Bukkit.getOnlinePlayers()) apply(player);
            if (visuals != null) visuals.refresh();
            result.complete(null);
        });
        return result;
    }

    /** Serializuje operacje administracyjne: zapis -> ponowny odczyt -> wyswietlenie. */
    public synchronized CompletableFuture<Void> change(Supplier<CompletableFuture<Void>> mutation) {
        CompletableFuture<Void> operation = mutationTail.handle((v, e) -> null)
                .thenCompose(unused -> mutation.get())
                .thenCompose(unused -> refresh());
        mutationTail = operation.handle((v, e) -> null);
        return operation;
    }

    public void expire() {
        if (stopping || expirePending) return;
        long now = System.currentTimeMillis();
        boolean due = snapshot.grants().values().stream()
                .anyMatch(grant -> grant.expiresAt() != null && grant.expiresAt() <= now);
        if (!due) return;

        // Natychmiast blokujemy uprawnienia wygaslych rang na glownym watku.
        for (Player player : Bukkit.getOnlinePlayers()) {
            RankSnapshot.Grant grant = snapshot.grants().get(player.getUniqueId());
            if (grant != null && !grant.active(now)) apply(player);
        }
        if (visuals != null) visuals.refresh();

        expirePending = true;
        change(() -> repository.expire(now)).whenComplete((unused, error) -> {
            if (!plugin.isEnabled()) return;
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                expirePending = false;
                if (error != null) plugin.getLogger().log(Level.WARNING,
                        "Nie udalo sie odswiezyc wygaslych rang.", error);
            });
        });
    }

    public void apply(Player player) {
        ThreadChecks.requirePrimaryThread();
        if (stopping || !player.isOnline()) return;
        UUID uuid = player.getUniqueId();

        PermissionAttachment old = attachments.remove(uuid);
        if (old != null) {
            try { player.removeAttachment(old); }
            catch (IllegalArgumentException ignored) { }
        }
        PermissionAttachment attachment = player.addAttachment(plugin);
        for (String permission : snapshot.permissionsFor(uuid)) {
            if (permission.equals("*")) {
                for (Permission available : plugin.getServer().getPluginManager().getPermissions()) {
                    attachment.setPermission(available.getName(), true);
                }
            } else {
                attachment.setPermission(permission, true);
            }
        }
        attachments.put(uuid, attachment);

        boolean all = snapshot.permissionsFor(uuid).contains("*");
        Boolean original = snapshot.opRestores().get(uuid);
        if (all && original == null && opPending.add(uuid)) {
            boolean before = player.isOp();
            repository.rememberOp(uuid, before)
                    .thenCompose(unused -> repository.load())
                    .thenCompose(this::installSnapshot)
                    .whenComplete((v, error) -> {
                        if (!plugin.isEnabled()) return;
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            opPending.remove(uuid);
                            if (error != null) plugin.getLogger().log(Level.SEVERE,
                                    "Nie zapisano stanu OP gracza " + player.getName(), error);
                        });
                    });
        } else if (all && original != null && !player.isOp()) {
            player.setOp(true);
        } else if (!all && original != null && opPending.add(uuid)) {
            player.setOp(original);
            repository.forgetOp(uuid).thenCompose(unused -> repository.load())
                    .thenCompose(this::installSnapshot).whenComplete((v, error) -> {
                        if (!plugin.isEnabled()) return;
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            opPending.remove(uuid);
                            if (error != null) plugin.getLogger().log(Level.SEVERE,
                                    "Nie mozna przywrocic operatora " + player.getName(), error);
                        });
                    });
        }
    }

    public void leave(Player player) {
        ThreadChecks.requirePrimaryThread();
        UUID uuid = player.getUniqueId();
        PermissionAttachment attachment = attachments.remove(uuid);
        if (attachment != null) {
            try { player.removeAttachment(attachment); }
            catch (IllegalArgumentException ignored) { }
        }
        Boolean original = snapshot.opRestores().get(uuid);
        if (original != null) player.setOp(original);
        if (visuals != null) visuals.leave(player);
    }

    public void stop() {
        ThreadChecks.requirePrimaryThread();
        for (Player player : Bukkit.getOnlinePlayers()) leave(player);
        stopping = true;
        if (visuals != null) visuals.stop();
    }
}

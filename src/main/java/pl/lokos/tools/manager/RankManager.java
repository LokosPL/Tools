package pl.lokos.tools.manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.java.JavaPlugin;
import pl.lokos.tools.database.RankRepository;
import pl.lokos.tools.config.DefinitionFiles;
import pl.lokos.tools.config.RanksFile;
import java.util.function.Function;
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
    private final DefinitionFiles definitions;
    private final Map<UUID, PermissionAttachment> attachments = new HashMap<>();
    private final Set<UUID> opPending = new HashSet<>();
    private volatile RankSnapshot snapshot = RankSnapshot.empty();
    private boolean stopping;
    private boolean expirePending;
    private RankVisualManager visuals;
    private CompletableFuture<Void> mutationTail = CompletableFuture.completedFuture(null);

    public RankManager(JavaPlugin plugin, RankRepository repository, DefinitionFiles definitions) {
        this.plugin = plugin;
        this.repository = repository;
        this.definitions = definitions;
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

    /**
     * Pierwszy odczyt bazy jest elementem kolejki operacji. Nie mozna
     * nadac rangi w trakcie inicjalizacji i potem nadpisac cache starym widokiem.
     */
    public synchronized void start() {
        mutationTail = (definitions.importRanks()?repository.load():repository.loadAssignments()).thenCompose(legacy -> {
            if(definitions.importRanks()){
                Map<String,RanksFile.RankEntry> imported=new LinkedHashMap<>(definitions.ranks().ranks());
                if(imported.isEmpty()){
                    for(var item:legacy.ranks().entrySet()){
                        RankSnapshot.Rank rank=item.getValue();
                        imported.put(item.getKey(),new RanksFile.RankEntry(
                                rank.prefix(),rank.suffix(),rank.position(),rank.joinMessage(),
                                legacy.permissions().getOrDefault(item.getKey(),Set.of())));
                    }
                }
                definitions.saveRanks(RanksFile.from(imported,definitions.ranks().settings()));
                plugin.getLogger().info("Rangi gotowe w Ranks.json: "+imported.size());
            }
            return refresh();
        }).whenComplete((ignored, error) -> {
            if (error != null) {
                plugin.getLogger().log(Level.SEVERE,
                        "Nie udało się pobrać początkowych rang z bazy MySQL.", error);
            }
        });
    }

    public CompletableFuture<Void> refresh() {
        return repository.loadAssignments().thenCompose(legacy -> {
            Map<String,RankSnapshot.Rank> ranks=new HashMap<>();
            Map<String,Set<String>> permissions=new HashMap<>();
            definitions.ranks().ranks().forEach((name,entry) -> {
                ranks.put(name,entry.toRank(name));
                permissions.put(name,entry.permissions());
            });
            return installSnapshot(new RankSnapshot(Map.copyOf(ranks),Map.copyOf(permissions),
                    legacy.grants(),legacy.opRestores()));
        });
    }

    private CompletableFuture<Void> installSnapshot(RankSnapshot next) {
        CompletableFuture<Void> result = new CompletableFuture<>();
        if (stopping || !plugin.isEnabled()) {
            result.completeExceptionally(new IllegalStateException("Plugin został wyłączony."));
            return result;
        }
        try {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                try {
                    snapshot = next;
                    Throwable problem = null;
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        try {
                            apply(player);
                        } catch (RuntimeException error) {
                            plugin.getLogger().log(Level.SEVERE,
                                    "Nie zaktualizowano uprawnień gracza " + player.getName(), error);
                            problem = error;
                        }
                    }
                    if (visuals != null) {
                        try {
                            visuals.refresh();
                        } catch (RuntimeException error) {
                            plugin.getLogger().log(Level.SEVERE,
                                    "Nie udało się odświeżyć TAB-u po zmianie rangi.", error);
                            if (problem == null) problem = error;
                        }
                    }
                    if (problem == null) result.complete(null);
                    else result.completeExceptionally(new IllegalStateException(
                            "Ranga zapisana w MySQL, ale aktualizacja uprawnień lub TAB-u nie powiodła się.", problem));
                } catch (Throwable error) {
                    result.completeExceptionally(error);
                    plugin.getLogger().log(Level.SEVERE, "Błąd aktualizacji rang.", error);
                }
            });
        } catch (RuntimeException error) {
            result.completeExceptionally(error);
        }
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

    private CompletableFuture<Void> updateDefinitions(Function<Map<String,RanksFile.RankEntry>,
            Map<String,RanksFile.RankEntry>> update) {
        return change(() -> CompletableFuture.runAsync(() -> {
            Map<String,RanksFile.RankEntry> changed=update.apply(new LinkedHashMap<>(definitions.ranks().ranks()));
            definitions.saveRanks(RanksFile.from(changed,definitions.ranks().settings()));
        }));
    }
    private static RanksFile.RankEntry require(Map<String,RanksFile.RankEntry> map,String name) {
        RanksFile.RankEntry rank=map.get(name);
        if(rank==null)throw new IllegalArgumentException("Ranga "+name+" nie istnieje.");
        return rank;
    }
    public CompletableFuture<Void> create(String name,String prefix,String suffix) {
        return updateDefinitions(map -> {
            if(map.containsKey(name))throw new IllegalArgumentException("Ta ranga już istnieje.");
            map.put(name,new RanksFile.RankEntry(prefix,suffix,null,"",Set.of()));
            return map;
        });
    }
    public CompletableFuture<Void> addPermission(String name,String permission) {
        return updateDefinitions(map -> {
            var r=require(map,name);
            Set<String> perms=new HashSet<>(r.permissions());perms.add(permission);
            map.put(name,new RanksFile.RankEntry(r.prefix(),r.suffix(),r.position(),r.joinMessage(),perms));
            return map;
        });
    }
    public CompletableFuture<Void> position(String name,int position){
        return updateDefinitions(map -> {
            var r=require(map,name);
            map.put(name,new RanksFile.RankEntry(r.prefix(),r.suffix(),position,r.joinMessage(),r.permissions()));
            return map;
        });
    }
    public CompletableFuture<Void> joinMessage(String name,String message){
        return updateDefinitions(map -> {
            var r=require(map,name);
            map.put(name,new RanksFile.RankEntry(r.prefix(),r.suffix(),r.position(),message,r.permissions()));
            return map;
        });
    }
    public CompletableFuture<Void> edit(String name,String field,String value) {
        if(field.equals("nazwa")){
            return change(() -> {
                Map<String,RanksFile.RankEntry> map=new LinkedHashMap<>(definitions.ranks().ranks());
                var rank=require(map,name);
                if(map.containsKey(value))throw new IllegalArgumentException("Ranga o nowej nazwie już istnieje.");
                // Najpierw zmieniamy przypisania w SQL, po powodzeniu JSON.
                return repository.renameGrants(name,value).thenRun(() -> {
                    map.remove(name);map.put(value,rank);
                    definitions.saveRanks(RanksFile.from(map,definitions.ranks().settings()));
                });
            });
        }
        return updateDefinitions(map -> {
            var r=require(map,name);
            map.put(name,new RanksFile.RankEntry(
                    field.equals("prefix")?value:r.prefix(),
                    field.equals("sufix")?value:r.suffix(),
                    r.position(),r.joinMessage(),r.permissions()));
            return map;
        });
    }
    public CompletableFuture<Void> delete(String name) {
        return change(() -> {
            Map<String,RanksFile.RankEntry> map=new LinkedHashMap<>(definitions.ranks().ranks());
            require(map,name);
            return repository.deleteGrants(name).thenRun(() -> {
                map.remove(name);
                definitions.saveRanks(RanksFile.from(map,definitions.ranks().settings()));
            });
        });
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
                    .thenCompose(unused -> refresh())
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
            repository.forgetOp(uuid).thenCompose(unused -> refresh()).whenComplete((v, error) -> {
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

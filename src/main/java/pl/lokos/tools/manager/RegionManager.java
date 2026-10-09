package pl.lokos.tools.manager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import pl.lokos.tools.database.RegionRepository;
import pl.lokos.tools.region.Region;
import pl.lokos.tools.region.RegionFlag;
import pl.lokos.tools.region.RegionIndex;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;
import java.util.logging.Level;

/** Wylacznie immutable cache w pamieci dla eventow; odczyt i zapis SQL w tle. */
public final class RegionManager {
    private final JavaPlugin plugin;
    private final RegionRepository repository;
    private final RankManager ranks;
    private volatile RegionIndex index = RegionIndex.empty();
    private volatile String mainSpawn;
    private volatile boolean loaded;
    private boolean closing;
    private CompletableFuture<Void> queue = CompletableFuture.completedFuture(null);
    private CompletableFuture<Void> initialLoad;

    public RegionManager(JavaPlugin plugin, RegionRepository repository, RankManager ranks) {
        this.plugin=plugin;this.repository=repository;this.ranks=ranks;
    }

    public synchronized CompletableFuture<Void> start() {
        if (initialLoad == null) {
            initialLoad=refresh();
            queue=initialLoad.handle((v,e)->null);
            initialLoad.exceptionally(e -> {
                plugin.getLogger().log(Level.SEVERE,"Nie załadowano regionów. Ochrona regionów niedostępna!",e);
                return null;
            });
        }
        return initialLoad;
    }

    public boolean ready() { return loaded; }
    public RegionIndex index() { return index; }
    public Region at(Location loc) {
        return index.at(loc.getWorld().getUID(),loc.getBlockX(),loc.getBlockZ());
    }
    public Region mainSpawn() { return mainSpawn==null?null:index.byName(mainSpawn); }

    public CompletableFuture<Void> refresh() {
        return repository.load().thenCompose(data -> {
            RegionIndex incoming = new RegionIndex(data.regions());
            CompletableFuture<Void> completion=new CompletableFuture<>();
            if (closing || !plugin.isEnabled()) {
                completion.completeExceptionally(new IllegalStateException("Plugin wyłączony."));
                return completion;
            }
            try {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    try {
                        index=incoming;
                        mainSpawn=data.mainSpawn();
                        loaded=true;
                        completion.complete(null);
                    } catch(Throwable e) {
                        completion.completeExceptionally(e);
                    }
                });
            } catch (RuntimeException e) {
                completion.completeExceptionally(e);
            }
            return completion;
        });
    }

    /** Uporzadkowanie zmian ogranicza wyscigi zapisu i odswiezania cache. */
    public synchronized CompletableFuture<Void> change(Supplier<CompletableFuture<Void>> action) {
        CompletableFuture<Void> task=queue.handle((v,e)->null)
                .thenCompose(unused-> {
                    if (closing) return CompletableFuture.failedFuture(new IllegalStateException("Plugin wyłączony."));
                    return action.get();
                }).thenCompose(unused->refresh());
        queue=task.handle((v,e)->null);
        return task;
    }

    public CompletableFuture<Void> create(Region region) {
        index.validateNew(region);
        return change(()->repository.create(region));
    }
    public CompletableFuture<Void> remove(String name) {
        return change(()->repository.remove(name));
    }
    public CompletableFuture<Void> flag(String name, RegionFlag f, Boolean value) {
        return change(()->repository.flag(name,f,value));
    }
    public CompletableFuture<Void> entryRank(String name,String rank) {
        return change(()->repository.entryRank(name,rank));
    }
    public CompletableFuture<Void> setSpawn(String name,Region.Spawn location) {
        return change(()->repository.setSpawn(name,location));
    }

    /** Op oraz wildcard rangi sa administracyjnym bypass wszystkich zabezpieczen. */
    public boolean bypass(Player player) {
        return player.isOp() || player.hasPermission("tools.region.bypass")
                || (ranks!=null && ranks.snapshot().permissionsFor(player.getUniqueId()).contains("*"));
    }
    public boolean allowed(Player player, Region region, RegionFlag flag) {
        return region==null || bypass(player) || index.enabled(region,flag);
    }

    public boolean canEnter(Player player,Region region) {
        if (region==null || bypass(player)) return true;
        RankSnapshot.Rank playerRank=ranks==null?null:ranks.snapshot().forPlayer(player.getUniqueId());
        Set<String> seen=new HashSet<>();
        while(region!=null && seen.add(region.name())) {
            if(region.entryRank()!=null) {
                RankSnapshot.Rank needed=ranks.snapshot().ranks().get(region.entryRank());
                if (needed==null || needed.position()==null || playerRank==null || playerRank.position()==null
                        || playerRank.position()>needed.position()) return false;
            }
            region=region.parent()==null?null:index.byName(region.parent());
        }
        return true;
    }

    public Location spawnOf(Region region) {
        if(region==null || region.spawn()==null) return null;
        var world=Bukkit.getWorld(region.world());
        if(world==null) return null;
        Region.Spawn s=region.spawn();
        return new Location(world,s.x(),s.y(),s.z(),s.yaw(),s.pitch());
    }

    public void shutdown() {closing=true;}
}

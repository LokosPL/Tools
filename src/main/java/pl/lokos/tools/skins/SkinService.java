package pl.lokos.tools.skins;

import com.destroystokyo.paper.profile.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tylko kosmetyczny podgląd skórki oficjalnego konta Minecraft o danym nicku.
 * NIGDY nie weryfikuje tożsamości logującego się gracza w offline-mode.
 * Nie wykonuje połączeń HTTP na wątku ticków.
 */
public final class SkinService implements Listener {
    private final JavaPlugin plugin;
    private final ConcurrentHashMap<String, Cached> cache = new ConcurrentHashMap<>();
    private final Set<String> inFlight = ConcurrentHashMap.newKeySet();
    private final AtomicInteger pending = new AtomicInteger();
    private final boolean enabled;
    private record Cached(PlayerProfile profile,long expires) {}

    public SkinService(JavaPlugin plugin,boolean enabled) {
        this.plugin=plugin;this.enabled=enabled;
    }
    @EventHandler public void join(PlayerJoinEvent event) {
        if(!enabled)return;
        Player player=event.getPlayer();
        String name=player.getName();
        if(!name.matches("[a-zA-Z0-9_]{3,16}"))return;
        String key=name.toLowerCase(Locale.ROOT);
        Cached cached=cache.get(key);
        if(cached!=null && cached.expires()>System.currentTimeMillis()) {
            apply(player,cached.profile());return;
        }
        if(pending.get()>=16 || !inFlight.add(key))return;
        pending.incrementAndGet();
        // createProfile musi zostać wykonane na głównym wątku, update jest asynchroniczne.
        PlayerProfile original = Bukkit.createProfile(name);
        original.update().orTimeout(4,TimeUnit.SECONDS).whenComplete((profile,error)->{
            inFlight.remove(key);pending.decrementAndGet();
            if(error!=null || profile==null || !profile.hasTextures() || !plugin.isEnabled())return;
            if(cache.size()>512)cache.clear();
            cache.put(key,new Cached(profile,System.currentTimeMillis()+6*60*60*1000L));
            Bukkit.getScheduler().runTask(plugin,()->{
                Player current=Bukkit.getPlayer(player.getUniqueId());
                if(current!=null && current.isOnline() && current.getName().equalsIgnoreCase(name))
                    apply(current,profile);
            });
        });
    }
    private void apply(Player player,PlayerProfile textureProfile) {
        try {
            PlayerProfile actual=player.getPlayerProfile();
            actual.setTextures(textureProfile.getTextures());
            player.setPlayerProfile(actual); // UUID offline pozostaje bez zmian.
        } catch (RuntimeException problem) {
            plugin.getLogger().fine("Nie udało się ustawić skórki dla "+player.getName());
        }
    }
}

package pl.lokos.tools.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import pl.lokos.tools.helpers.Colors;
import pl.lokos.tools.manager.RegionManager;
import pl.lokos.tools.manager.RegionTeleportManager;
import pl.lokos.tools.region.Region;

/** Pierwsze wejscie, respawn i actionbar aktualnego podregionu. */
public final class RegionPlayerListener implements Listener {
    private final JavaPlugin plugin;
    private final RegionManager regions;
    private final RegionTeleportManager teleports;
    private final String title;

    public RegionPlayerListener(JavaPlugin plugin,RegionManager regions,RegionTeleportManager teleports,String title) {
        this.plugin=plugin;this.regions=regions;this.teleports=teleports;this.title=title;
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void join(PlayerJoinEvent event) {
        if(event.getPlayer().hasPlayedBefore())return;
        var player=event.getPlayer();
        regions.start().whenComplete((v,error)-> {
            if(error!=null || !plugin.isEnabled())return;
            Bukkit.getScheduler().runTask(plugin,()->{
                if(!player.isOnline())return;
                Region spawn=regions.mainSpawn();
                Location target=regions.spawnOf(spawn);
                if(target!=null && (spawn==null || regions.canEnter(player,spawn)))
                    player.teleportAsync(target); // nie wczytujemy chunkow synchronicznie
            });
        });
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void respawn(PlayerRespawnEvent event) {
        Region spawn=regions.mainSpawn();
        Location loc=regions.spawnOf(spawn);
        if(loc!=null && (spawn==null || regions.canEnter(event.getPlayer(),spawn)))event.setRespawnLocation(loc);
    }

    @EventHandler
    public void quit(PlayerQuitEvent e) {teleports.cancel(e.getPlayer(),false);}

    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void hurt(EntityDamageEvent e) {
        if(e.getEntity() instanceof Player p)teleports.cancel(p,true);
    }

    public void actionbar() {
        if(!regions.ready())return;
        for(Player player:Bukkit.getOnlinePlayers()) {
            if(teleports.busy(player)) continue;
            Region r=regions.visibleAt(player.getLocation());
            if(r!=null) {
                String parent=r.parent();
                String path=parent==null?"&a"+r.name()
                        :"&a"+parent+" &8→ &a"+r.name();
                if(regions.inHalo(player.getLocation()))
                    path="&a"+r.name()+" &8→ &7strefa ochronna";
                player.sendActionBar(Colors.color(title+path));
            }
        }
    }
}

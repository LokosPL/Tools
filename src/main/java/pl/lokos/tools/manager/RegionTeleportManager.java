package pl.lokos.tools.manager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import pl.lokos.tools.config.ToolsConfig;
import pl.lokos.tools.helpers.Colors;
import pl.lokos.tools.helpers.Messages;
import pl.lokos.tools.region.Region;

import java.util.*;

/** Czasowe teleportacje bez synchronicznego wczytywania chunkow. */
public final class RegionTeleportManager {
    private final JavaPlugin plugin;
    private final RegionManager regions;
    private final RankManager ranks;
    private final ToolsConfig.Regions settings;
    private final Map<UUID, Pending> pending=new HashMap<>();
    private PlayerStatusBar statusBar;
    public void setStatusBar(PlayerStatusBar statusBar) { this.statusBar=statusBar; }
    private void notice(Player player,String message) {
        if (statusBar!=null) statusBar.notice(player,message,1200);
    }

    private record Pending(BukkitTask task, Location origin) {}

    public RegionTeleportManager(JavaPlugin plugin,RegionManager regions,RankManager ranks,ToolsConfig.Regions settings) {
        this.plugin=plugin;this.regions=regions;this.ranks=ranks;this.settings=settings;
    }

    public boolean busy(Player player) {return pending.containsKey(player.getUniqueId());}

    public void start(Player player, String name) {
        Region region=regions.index().byName(name);
        if(region==null || region.spawn()==null) {Messages.error(player,"Ta lokalizacja nie ma punktu teleportacji.");return;}
        if(!regions.canEnter(player,region)) {Messages.error(player,"Nie masz rangi wymaganej do tej lokalizacji.");return;}
        Location target=regions.spawnOf(region);
        if(target==null) {Messages.error(player,"Świat tej lokalizacji nie jest dostępny.");return;}
        cancel(player,false);
        // Teleportacja natychmiastowa wymaga faktycznej permisji.
        if (instantAllowed(ranks.snapshot(), player.getUniqueId(), player.isOp())) {
            teleportNow(player,name);
            return;
        }
        Location origin=player.getLocation().clone();
        final int seconds=settings.teleportSeconds();
        BukkitRunnable job=new BukkitRunnable() {
            private int remaining=seconds;
            @Override public void run() {
                if(!player.isOnline()) {finish();return;}
                if(settings.cancelTeleportOnMove() &&
                        (!player.getWorld().equals(origin.getWorld())
                        || player.getLocation().distanceSquared(origin)>0.3)) {
                    RegionTeleportManager.this.cancel(player,true);
                    return;
                }
                Region fresh=regions.index().byName(name);
                if(fresh==null || !regions.canEnter(player,fresh)) {
                    RegionTeleportManager.this.cancel(player,true); return;
                }
                if(remaining==0) {
                    finish();
                    notice(player,"&#86E6BCTeleportacja &8» &7Trwa przenoszenie…");
                    // Paper teleportAsync wczytuje chunk bez blokowania tickow.
                    teleportNow(player,name);
                    return;
                }
                notice(player,"&#86E6BCTeleportacja: &7"+name+" &8• &#FFD166"+remaining+" s");
                player.playSound(player.getLocation(),Sound.BLOCK_NOTE_BLOCK_HAT,0.4f,1.2f);
                player.getWorld().spawnParticle(Particle.END_ROD,player.getLocation().add(0,0.15,0),8,0.4,0.12,0.4,0.025);
                remaining--;
            }
            private void finish() {pending.remove(player.getUniqueId());cancel();}
        };
        BukkitTask task=job.runTaskTimer(plugin,0L,20L);
        pending.put(player.getUniqueId(),new Pending(task,origin));
    }

    /** Ta sama polityka uprawnień dla /spawn i menu lokalizacji. */
    public static boolean instantAllowed(RankSnapshot snapshot, UUID uuid, boolean operator) {
        return pl.lokos.tools.security.ToolsAccess.allowed(
                operator, snapshot, uuid, "tools.lokalizacje.instant", false);
    }

    private void teleportNow(Player player,String name){
        Region current = regions.index().byName(name);
        if (current == null || !regions.canEnter(player,current)) {
            Messages.error(player,"Lokalizacja jest już niedostępna.");
            return;
        }
        Location updatedTarget = regions.spawnOf(current);
        if (updatedTarget == null) {
            Messages.error(player,"Punkt teleportacji tej lokalizacji jest niedostępny.");
            return;
        }
        player.teleportAsync(updatedTarget).whenComplete((done,error)->{
            if(!plugin.isEnabled())return;
            Bukkit.getScheduler().runTask(plugin,()->{
                if(!player.isOnline())return;
                if(error!=null || !Boolean.TRUE.equals(done)) {
                    Messages.error(player,"Teleportacja nie powiodła się.");
                }else{
                    Messages.success(player,"Teleportowano do &a"+name+"&7.");
                    player.getWorld().spawnParticle(Particle.PORTAL,
                            player.getLocation().add(0,1,0),28,0.35,0.6,0.35,0.08);
                    player.playSound(player.getLocation(),Sound.ENTITY_ENDERMAN_TELEPORT,0.65f,1.2f);
                }
            });
        });
    }

    public void cancel(Player player,boolean notify) {
        Pending previous=pending.remove(player.getUniqueId());
        if(previous!=null) {
            previous.task().cancel();
            if(notify && player.isOnline())
                notice(player,"&#FF737F✘ &7Teleportacja przerwana");
        }
    }

    public void cancelAll() {
        for(Pending p:pending.values())p.task().cancel();
        pending.clear();
    }
}

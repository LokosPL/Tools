package pl.lokos.tools.manager;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import pl.lokos.tools.region.Region;
import pl.lokos.tools.region.RegionBorderSamples;
import pl.lokos.tools.helpers.Colors;
import java.util.*;

/**
 * Gesty podglad dokladnej granicy blisko gracza. Wysylany tylko temu graczowi,
 * przez 10 sekund; zadnych fake blokow, zmian swiata ani skanowania chunkow.
 */
public final class RegionBorderPreview {
    private static final double RANGE = 36.0;
    private static final double SPACING = 1.5;
    private static final int MAX_POINTS = 140;
    private static final Particle.DustOptions DUST =
            new Particle.DustOptions(Color.fromRGB(70, 235, 155), 1.3f);

    private final JavaPlugin plugin;
    private final Map<UUID,BukkitRunnable> active=new HashMap<>();
    public RegionBorderPreview(JavaPlugin plugin){this.plugin=plugin;}

    public void preview(Player player,Region region) {
        BukkitRunnable previous=active.remove(player.getUniqueId());
        if(previous!=null)previous.cancel();
        World world=Bukkit.getWorld(region.world());
        if(world==null || !world.equals(player.getWorld()))return;

        if(RegionBorderSamples.nearby(region,player.getX(),player.getZ(),
                RANGE,SPACING,MAX_POINTS).isEmpty()){
            player.sendMessage(Colors.color("&7Granica regionu &a"+region.name()
                    +" &7jest dalej niż &a36 bloków&7. Podejdź bliżej i ponów podgląd."));
            return;
        }
        player.sendMessage(Colors.color("&aGranice regionu &8» &7"+region.name()
                +" &8• &7Podgląd przez &a10 sekund&7."));
        BukkitRunnable job=new BukkitRunnable(){
            private int cycles;
            @Override public void run(){
                if(!player.isOnline() || !world.equals(player.getWorld()) || cycles++>=20){
                    active.remove(player.getUniqueId());cancel();return;
                }
                // Zawsze obliczaj tylko okolice gracza. Rozmiar regionu nie zwieksza
                // zlozonosci; maksymalnie 140 punktow × 3 wysokosci / 10 tickow.
                var points=RegionBorderSamples.nearby(region,player.getX(),player.getZ(),
                        RANGE,SPACING,MAX_POINTS);
                double y=player.getY();
                for(var point:points){
                    for(double height:new double[]{0.35,1.55,2.75}){
                        player.spawnParticle(Particle.DUST,point.x(),y+height,point.z(),
                                1,0,0,0,0,DUST);
                    }
                }
            }
        };
        active.put(player.getUniqueId(),job);
        job.runTaskTimer(plugin,0L,10L);
    }

    public void shutdown(){
        for(BukkitRunnable job:active.values())job.cancel();
        active.clear();
    }
}

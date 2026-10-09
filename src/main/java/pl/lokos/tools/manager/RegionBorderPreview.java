package pl.lokos.tools.manager;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import pl.lokos.tools.region.Region;
import pl.lokos.tools.helpers.Colors;
import java.util.*;

/** Wylacznie czasteczki wysylane do jednego gracza przez 10 sekund, bez fikcyjnych barier. */
public final class RegionBorderPreview {
    private final JavaPlugin plugin;
    private final Map<UUID,BukkitRunnable> active=new HashMap<>();
    public RegionBorderPreview(JavaPlugin plugin){this.plugin=plugin;}
    public void preview(Player player,Region region){
        BukkitRunnable previous=active.remove(player.getUniqueId());
        if(previous!=null)previous.cancel();
        World world=Bukkit.getWorld(region.world());
        if(world==null || !world.equals(player.getWorld()))return;
        player.sendActionBar(Colors.color("&aɢʀᴀɴɪᴄᴇ &8» &7"+region.name()+" &8• &a10 s"));
        BukkitRunnable job=new BukkitRunnable(){
            private int ticks;
            @Override public void run(){
                if(!player.isOnline() || !world.equals(player.getWorld())||ticks++>=20){
                    active.remove(player.getUniqueId());cancel();return;
                }
                double y=player.getY()+0.4;
                // Maksymalnie 40 punktow raz na 10 tickow, klient otrzymuje tylko pobliskie punkty.
                int segments=10;
                for(int i=0;i<=segments;i++){
                    double t=i/(double)segments;
                    point(region.minX()+t*(region.maxX()-region.minX()),y,region.minZ());
                    point(region.minX()+t*(region.maxX()-region.minX()),y,region.maxZ()+1);
                    point(region.minX(),y,region.minZ()+t*(region.maxZ()-region.minZ()));
                    point(region.maxX()+1,y,region.minZ()+t*(region.maxZ()-region.minZ()));
                }
            }
            private void point(double x,double y,double z){
                if(Math.abs(player.getX()-x)>48 || Math.abs(player.getZ()-z)>48)return;
                player.spawnParticle(Particle.END_ROD,x+0.5,y,z+0.5,1,0,0,0,0);
            }
        };
        active.put(player.getUniqueId(),job);
        job.runTaskTimer(plugin,0L,10L);
    }
    public void shutdown(){for(BukkitRunnable job:active.values())job.cancel();active.clear();}
}

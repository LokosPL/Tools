package pl.lokos.tools.combat;

import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.plugin.java.JavaPlugin;
import pl.lokos.tools.config.JsonConfigManager;
import pl.lokos.tools.helpers.Colors;
import pl.lokos.tools.manager.RegionManager;
import pl.lokos.tools.region.Region;
import pl.lokos.tools.staff.StaffManager;

import java.io.IOException;
import java.util.*;

/**
 * Jedno źródło combat-taga: PvP i aktywne ataki mobów, żadnych tagów
 * od lawy, upadku, własnej strzały lub cancelowanych ataków.
 */
public final class CombatManager implements Listener {
    private record Tagged(long until,BossBar bar) {}
    private final JavaPlugin plugin;
    private final RegionManager regions;
    private final StaffManager staff;
    private final CombatConfig settings;
    private final Map<UUID,Tagged> tagged=new HashMap<>();
    private final Set<UUID> logoutDeaths=new HashSet<>();
    public CombatManager(JavaPlugin plugin,RegionManager regions,StaffManager staff,
                         java.nio.file.Path directory) throws IOException {
        this.plugin=plugin;this.regions=regions;this.staff=staff;
        settings=new JsonConfigManager(directory).load("Combat.json",CombatConfig.class,
                CombatConfig::new,CombatConfig::validate);
        if(settings.disableAdvancementAnnouncements()){
            for(World world:Bukkit.getWorlds())disableAdvancements(world);
        }
    }
    public CombatConfig settings(){return settings;}
    private void disableAdvancements(World world){
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS,false);
    }
    @EventHandler public void world(WorldLoadEvent event){
        if(settings.disableAdvancementAnnouncements())disableAdvancements(event.getWorld());
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void damage(EntityDamageByEntityEvent event){
        if(!settings.enabled()||event.getFinalDamage()<=0)return;
        Entity source=event.getDamager();
        if(source instanceof Projectile projectile){
            ProjectileSource shooter=projectile.getShooter();
            if(shooter instanceof Entity entity)source=entity;
            else return;
        }
        if(!(source instanceof LivingEntity living))return;
        if(event.getEntity() instanceof Player victim)
            tag(victim,living,event.isCancelled());
        if(living instanceof Player attacker && event.getEntity() instanceof LivingEntity target)
            if(!target.equals(attacker) && CombatRules.shouldTagAttacker(
                    target instanceof Player,target instanceof Enemy))
                tag(attacker,target,event.isCancelled());
    }
    private void tag(Player victim,LivingEntity attacker,boolean cancelled){
        if(!CombatRules.shouldTag(cancelled,true,!victim.equals(attacker),true,
                attacker instanceof Player,settings.tagFromPlayers(),settings.tagFromMobs()))return;
        long now=System.currentTimeMillis();
        Tagged before=tagged.get(victim.getUniqueId());
        BossBar bar=before!=null?before.bar():BossBar.bossBar(
                Colors.color(settings.bossbar().replace("{seconds}",String.valueOf(settings.tagSeconds()))),
                1f,BossBar.Color.RED,BossBar.Overlay.PROGRESS);
        if(before==null)victim.showBossBar(bar);
        tagged.put(victim.getUniqueId(),new Tagged(now+settings.tagSeconds()*1000L,bar));
    }
    public boolean tagged(UUID id,long now){
        Tagged tag=tagged.get(id);
        return tag!=null&&tag.until()>now;
    }
    public void tick(){
        long now=System.currentTimeMillis();
        for(var iter=tagged.entrySet().iterator();iter.hasNext();){
            var current=iter.next();
            Player player=Bukkit.getPlayer(current.getKey());
            Tagged tag=current.getValue();
            if(player==null || !player.isOnline() || now>=tag.until()){
                if(player!=null)player.hideBossBar(tag.bar());
                iter.remove();continue;
            }
            int seconds=(int)Math.ceil((tag.until()-now)/1000d);
            tag.bar().name(Colors.color(settings.bossbar()
                    .replace("{seconds}",String.valueOf(seconds))));
            tag.bar().progress(Math.min(1f,Math.max(0f,
                    (tag.until()-now)/(settings.tagSeconds()*1000f))));
        }
    }
    private void remove(Player p){
        Tagged current=tagged.remove(p.getUniqueId());
        if(current!=null)p.hideBossBar(current.bar());
    }
    @EventHandler(priority=EventPriority.HIGH)
    public void quit(PlayerQuitEvent event){
        Player player=event.getPlayer();
        if(settings.logoutKillsPlayer()&&tagged(player.getUniqueId(),System.currentTimeMillis())
                && !player.isDead()){
            logoutDeaths.add(player.getUniqueId());
            // Zgon na serwerze (przed rozłączeniem) zachowuje standardowy
            // mechanizm dropu ekwipunku, bez modyfikacji NBT.
            player.setHealth(0.0);
            for(Player online:Bukkit.getOnlinePlayers()){
                if(staff!=null&&staff.vanished(player)&&!staff.see(online))continue;
                online.sendMessage(Colors.color(settings.logout().replace("{player}",player.getName())));
            }
        }
        remove(player);logoutDeaths.remove(player.getUniqueId());
    }
    @EventHandler(priority=EventPriority.HIGHEST)
    public void death(PlayerDeathEvent event){
        Player player=event.getEntity();
        remove(player);
        if(logoutDeaths.contains(player.getUniqueId())||
                staff!=null&&staff.vanished(player)){
            event.deathMessage(null);return;
        }
        EntityDamageEvent damage=player.getLastDamageCause();
        String reason=CombatRules.reason(damage==null?null:damage.getCause());
        event.deathMessage(Colors.color(settings.death().replace("{player}",player.getName())
                .replace("{reason}",reason)));
    }
    @EventHandler(priority=EventPriority.HIGH)
    public void respawn(PlayerRespawnEvent event){
        if(!settings.respawnAtMainSpawn()
                ||event.getRespawnReason()!=PlayerRespawnEvent.RespawnReason.DEATH)return;
        Location spawn=null;
        if(regions!=null){
            Region region=regions.mainSpawn();
            if(region!=null)spawn=regions.spawnOf(region);
        }
        if(spawn==null){
            // Gdy region Spawn nie został jeszcze skonfigurowany, wybierz
            // główny świat serwera zamiast bieżącego Netheru lub Endu.
            World fallback=Bukkit.getWorlds().stream()
                    .filter(world->world.getEnvironment()==World.Environment.NORMAL)
                    .findFirst().orElse(event.getPlayer().getWorld());
            spawn=fallback.getSpawnLocation();
        }
        event.setRespawnLocation(spawn);
    }
    public void shutdown(){
        for(Player player:Bukkit.getOnlinePlayers())remove(player);
        tagged.clear();logoutDeaths.clear();
    }
}

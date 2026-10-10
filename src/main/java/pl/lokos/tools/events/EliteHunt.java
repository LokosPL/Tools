package pl.lokos.tools.events;

import org.bukkit.*;
import org.bukkit.entity.Husk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import pl.lokos.tools.helpers.Colors;
import pl.lokos.tools.manager.RegionManager;
import pl.lokos.tools.region.RegionFlag;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Kontrolowana fala elitarnych potworów. Brak zmian terenu, brak generowania
 * chunków, brak spawnu w regionach i brak upuszczania ekwipunku elit.
 */
public final class EliteHunt implements Listener,AutoCloseable {
    private record Elite(Husk entity,UUID world,int x,int z,long expiresAt) {}
    private final JavaPlugin plugin;
    private final EventManager events;
    private final EventConfig config;
    private final RegionManager regions;
    private final Map<UUID,Elite> elites=new LinkedHashMap<>();
    private long sessionStart;
    private long nextSpawnAt;

    public EliteHunt(JavaPlugin plugin,EventManager events,
                     EventConfig config,RegionManager regions){
        this.plugin=plugin;this.events=events;this.config=config;this.regions=regions;
    }
    public int activeCount(){return elites.size();}

    public void tick(){
        if(!config.eliteEnabled()||events.active()!=EventType.LOWY||
                regions==null||!regions.ready()){
            clear();sessionStart=0;nextSpawnAt=0;return;
        }
        long now=System.currentTimeMillis(),started=events.state().startedAt();
        if(sessionStart!=started){
            clear();sessionStart=started;nextSpawnAt=now+5000L;
        }
        for(var it=elites.entrySet().iterator();it.hasNext();){
            Elite elite=it.next().getValue();
            Husk mob=elite.entity();
            World world=Bukkit.getWorld(elite.world());
            if(now>=elite.expiresAt()||world==null||
                    !world.isChunkLoaded(mob.getLocation().getBlockX()>>4,
                            mob.getLocation().getBlockZ()>>4)||
                    !mob.isValid()||!eligiblePosition(mob.getLocation())){
                if(mob.isValid())mob.remove();
                it.remove();
            }else if(config.eliteEffects()){
                Location loc=mob.getLocation().add(0,1.3,0);
                if(world.getPlayers().stream().anyMatch(player->
                        player.getLocation().distanceSquared(loc)<32*32))
                    world.spawnParticle(Particle.SOUL_FIRE_FLAME,loc,
                            6,0.4,0.5,0.4,0.007);
            }
        }
        if(now<nextSpawnAt||elites.size()>=config.eliteMaxActive())return;
        nextSpawnAt=now+config.eliteSpawnIntervalSeconds()*1000L;
        spawn(now);
    }

    private boolean eligiblePosition(Location loc){
        if(loc==null||loc.getWorld()==null||!regions.ready())return false;
        World world=loc.getWorld();
        return MeteorSpawnPolicy.unprotected(regions.index(),regions.mainSpawn(),
                world.getUID(),loc.getBlockX(),loc.getBlockZ(),32)
                && !regions.inHalo(loc)
                && !regions.protectedLocation(loc,RegionFlag.MOBS)
                && !regions.protectedLocation(loc,RegionFlag.DAMAGE)
                && world.getWorldBorder().isInside(loc);
    }

    private void spawn(long now){
        List<Player> possible=new ArrayList<>();
        for(Player player:Bukkit.getOnlinePlayers())
            if(player.getGameMode()==GameMode.SURVIVAL &&
                    player.getWorld().getEnvironment()==World.Environment.NORMAL)
                possible.add(player);
        if(possible.isEmpty())return;
        Collections.shuffle(possible);
        int players=0;
        for(Player player:possible){
            if(players++>=8)return;
            World world=player.getWorld();
            for(int n=0;n<8;n++){
                int x=player.getLocation().getBlockX()+ThreadLocalRandom.current().nextInt(-40,41);
                int z=player.getLocation().getBlockZ()+ThreadLocalRandom.current().nextInt(-40,41);
                int dx=x-player.getLocation().getBlockX(),dz=z-player.getLocation().getBlockZ();
                if(dx*dx+dz*dz<16*16||!world.isChunkLoaded(x>>4,z>>4))continue;
                int y=world.getHighestBlockYAt(x,z);
                if(y<world.getMinHeight()+2||y+3>=world.getMaxHeight()||
                        !world.getBlockAt(x,y,z).getType().isSolid()||
                        !world.getBlockAt(x,y+1,z).getType().isAir()||
                        !world.getBlockAt(x,y+2,z).getType().isAir())continue;
                Location loc=new Location(world,x+0.5,y+1,z+0.5);
                if(!eligiblePosition(loc))continue;
                create(loc,player,now);
                return;
            }
        }
    }

    private void create(Location loc,Player target,long now){
        World world=loc.getWorld();
        if(world==null)return;
        Husk mob=world.spawn(loc,Husk.class,elite->{
            elite.setPersistent(false);
            elite.setRemoveWhenFarAway(false);
            elite.customName(Colors.color("&#FF727F☠ TYTAN &#FFD166✦ Łowy"));
            elite.setCustomNameVisible(true);
            elite.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE,
                    config.eliteLifetimeSeconds()*20,0,false,false));
            elite.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH,
                    config.eliteLifetimeSeconds()*20,0,false,false));
            EntityEquipment equipment=elite.getEquipment();
            if(equipment!=null){
                equipment.setItemInMainHand(new ItemStack(Material.IRON_SWORD));
                equipment.setItemInMainHandDropChance(0f);
                equipment.setHelmet(new ItemStack(Material.IRON_HELMET));
                equipment.setHelmetDropChance(0f);
            }
        });
        mob.setTarget(target);
        elites.put(mob.getUniqueId(),new Elite(mob,world.getUID(),
                loc.getBlockX(),loc.getBlockZ(),
                now+config.eliteLifetimeSeconds()*1000L));
        if(config.eliteAnnouncements())
            Bukkit.broadcast(Colors.color("&#FF727F☠ Tytan pojawił się! &#A8A8B7Świat: &#70D6E8"
                    +world.getName()+" &#A8A8B7X: &#FFD166"+loc.getBlockX()
                    +" &#A8A8B7Z: &#FFD166"+loc.getBlockZ()
                    +" &#70D6E8| /lowy"));
        world.playSound(loc,Sound.ENTITY_WITHER_SPAWN,0.8f,1.25f);
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onKill(EntityDeathEvent event){
        Elite elite=elites.remove(event.getEntity().getUniqueId());
        if(elite==null)return;
        // Po zabiciu potwora nie pozostawiamy waniliowych dropów;
        // wyłącznie gwarantowane nagrody Tools, bez farmy żelaznego sprzętu.
        event.getDrops().clear();
        event.setDroppedExp(0);
        if(events.active()!=EventType.LOWY||!config.eliteEnabled())return;
        Player killer=event.getEntity().getKiller();
        if(killer==null||killer.getGameMode()!=GameMode.SURVIVAL
                ||!eligiblePosition(event.getEntity().getLocation()))return;
        events.eliteDefeated(killer,config.eliteKeysPerKill());
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void chunkUnload(ChunkUnloadEvent event){
        for(var it=elites.entrySet().iterator();it.hasNext();){
            Elite elite=it.next().getValue();
            if(elite.world().equals(event.getWorld().getUID())
                    && (elite.entity().getLocation().getBlockX()>>4)==event.getChunk().getX()
                    && (elite.entity().getLocation().getBlockZ()>>4)==event.getChunk().getZ()){
                if(elite.entity().isValid())elite.entity().remove();
                it.remove();
            }
        }
    }
    private void clear(){
        for(Elite elite:elites.values())if(elite.entity().isValid())elite.entity().remove();
        elites.clear();
    }
    @Override public void close(){clear();}
}

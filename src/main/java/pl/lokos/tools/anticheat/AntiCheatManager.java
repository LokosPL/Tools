package pl.lokos.tools.anticheat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import pl.lokos.tools.config.JsonConfigManager;
import pl.lokos.tools.helpers.Colors;
import pl.lokos.tools.helpers.StateChanges;
import pl.lokos.tools.manager.RankManager;
import pl.lokos.tools.security.ToolsAccess;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Ostrożny antycheat: każda postać (także OP) podlega tym samym kontrolom;
 * brak automatycznych kicków/banów. Ochrona TPS działa niezależnie od
 * przełącznika wykrywania i ma osobne wysokie progi.
 */
public final class AntiCheatManager implements Listener, AutoCloseable {
    private record Motion(long graceUntil,int violations,long lastTime) {}
    private record ChunkKey(UUID world,int x,int z) {}
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final JavaPlugin plugin;
    private final RankManager ranks;
    private final Path file;
    private final AntiCheatConfig settings;
    private final ExecutorService writes=Executors.newSingleThreadExecutor(r->{
        Thread t=new Thread(r,"Tools-AntiCheat-Zapis");t.setDaemon(false);return t;
    });
    private volatile AntiCheatState state;
    private final Map<UUID,Motion> motion=new HashMap<>();
    private final RateWindow<UUID> placeBurst=new RateWindow<>(1000,5000);
    private final RateWindow<ChunkKey> redstone=new RateWindow<>(1000,10000);
    private final RateWindow<ChunkKey> tnt=new RateWindow<>(5000,10000);
    private final RateWindow<ChunkKey> spawn=new RateWindow<>(5000,10000);
    private final Map<String,Long> lastAlerts=new HashMap<>();
    private long lastCleanup;

    public AntiCheatManager(JavaPlugin plugin,RankManager ranks,Path folder) throws IOException {
        this.plugin=plugin;this.ranks=ranks;this.file=folder.resolve("AntiCheatState.json");
        JsonConfigManager json=new JsonConfigManager(folder);
        try{
            settings=json.load("AntiCheat.json",AntiCheatConfig.class,
                    AntiCheatConfig::new,AntiCheatConfig::validate);
            state=json.load("AntiCheatState.json",AntiCheatState.class,
                    AntiCheatState::new,AntiCheatState::validate);
        }catch(IOException error){writes.shutdown();throw error;}
    }
    public AntiCheatConfig settings(){return settings;}
    public AntiCheatState state(){return state;}

    public synchronized CompletableFuture<Void> enabled(boolean value){
        if(state.enabled()==value)return CompletableFuture.failedFuture(new StateChanges.Unchanged(
                value?"Antycheat jest już włączony.":"Antycheat jest już wyłączony."));
        state=state.withEnabled(value);
        return save(state);
    }
    public synchronized CompletableFuture<Void> alerts(UUID player,boolean value){
        if(state.receives(player)==value)return CompletableFuture.failedFuture(new StateChanges.Unchanged(
                value?"Powiadomienia antycheata są już włączone.":"Powiadomienia antycheata są już wyłączone."));
        state=state.withAlerts(player,value);
        return save(state);
    }
    private CompletableFuture<Void> save(AntiCheatState snapshot){
        return CompletableFuture.runAsync(()->{
            Path tmp=null;
            try{
                tmp=Files.createTempFile(file.getParent(),".tools-anticheat-",".tmp");
                Files.writeString(tmp,GSON.toJson(snapshot)+"\n",StandardCharsets.UTF_8);
                try{Files.move(tmp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
                catch(AtomicMoveNotSupportedException ignored){
                    Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING);
                }
            }catch(IOException failure){throw new CompletionException(failure);}
            finally{if(tmp!=null)try{Files.deleteIfExists(tmp);}catch(IOException ignored){}}
        },writes);
    }
    private void alert(String player,String check,String details){
        String key=player+":"+check;
        long now=System.currentTimeMillis();
        if(now-lastAlerts.getOrDefault(key,0L)<settings.alertCooldownSeconds()*1000L)return;
        lastAlerts.put(key,now);
        String raw=settings.alertFormat().replace("{player}",player).replace("{check}",check)
                .replace("{details}",details);
        for(Player viewer:Bukkit.getOnlinePlayers())
            if(state.receives(viewer.getUniqueId()) &&
                    ToolsAccess.allowed(viewer,ranks,"tools.antycheat.alerts",false))
                viewer.sendMessage(Colors.color(raw));
        plugin.getLogger().warning("[AntiCheat] "+player+" "+check+": "+details);
    }
    private static ChunkKey chunk(Location loc){
        return new ChunkKey(loc.getWorld().getUID(),loc.getBlockX()>>4,loc.getBlockZ()>>4);
    }
    private static boolean motionExempt(Player player){
        if(player.isGliding() || player.isFlying() || player.getAllowFlight()
                || player.isInsideVehicle()||player.isSwimming()||player.isRiptiding()
                || player.getGameMode()==org.bukkit.GameMode.CREATIVE
                ||player.getGameMode()==org.bukkit.GameMode.SPECTATOR)return true;
        if(player.hasPotionEffect(PotionEffectType.SPEED)
                ||player.hasPotionEffect(PotionEffectType.LEVITATION)
                ||player.hasPotionEffect(PotionEffectType.DOLPHINS_GRACE)
                ||player.hasPotionEffect(PotionEffectType.JUMP_BOOST))return true;
        Material block=player.getLocation().getBlock().getType();
        return block==Material.WATER ||block==Material.LAVA ||block==Material.COBWEB
                ||block==Material.BUBBLE_COLUMN ||block==Material.SOUL_SAND;
    }
    public void grace(Player player){
        motion.put(player.getUniqueId(),new Motion(System.currentTimeMillis()+
                settings.joinGraceSeconds()*1000L,0,System.currentTimeMillis()));
    }
    @EventHandler public void join(PlayerJoinEvent event){grace(event.getPlayer());}
    @EventHandler public void teleport(PlayerTeleportEvent event){grace(event.getPlayer());}
    @EventHandler public void velocity(PlayerVelocityEvent event){grace(event.getPlayer());}
    @EventHandler public void quit(PlayerQuitEvent event){
        UUID id=event.getPlayer().getUniqueId();
        motion.remove(id);placeBurst.remove(id);
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void damaged(EntityDamageEvent event){
        if(event.getEntity() instanceof Player player)grace(player);
    }
    @EventHandler(priority=EventPriority.HIGH,ignoreCancelled=true)
    public void movement(PlayerMoveEvent event){
        if(!state.enabled()||!settings.movementCheck()||event.getTo()==null)return;
        Location from=event.getFrom(),to=event.getTo();
        if(!from.getWorld().equals(to.getWorld()))return;
        double dx=to.getX()-from.getX(),dz=to.getZ()-from.getZ();
        if(dx==0&&dz==0)return;
        Player player=event.getPlayer();
        long now=System.currentTimeMillis();
        Motion previous=motion.get(player.getUniqueId());
        if(previous==null){grace(player);return;}
        if(now<previous.graceUntil()||motionExempt(player)||Bukkit.getTPS()[0]<18){
            motion.put(player.getUniqueId(),new Motion(previous.graceUntil(),0,now));
            return;
        }
        // Wyłącznie skrajnie anomalny ruch >1.8 bloku per event przez wiele eventów.
        double horizontal=dx*dx+dz*dz;
        int violations=horizontal>settings.maximumHorizontalPerMove()*
                settings.maximumHorizontalPerMove()?
                (now-previous.lastTime()>2000?1:previous.violations()+1):
                Math.max(0,previous.violations()-2);
        motion.put(player.getUniqueId(),new Motion(previous.graceUntil(),violations,now));
        if(violations>=settings.movementViolations()){
            alert(player.getName(),"Podejrzany ruch","seria "+violations+" przekroczeń");
            // Anulujemy pojedynczy ruch dopiero po powtarzalnej anomalii.
            event.setTo(from);
        }
    }
    @EventHandler(priority=EventPriority.HIGH,ignoreCancelled=true)
    public void reach(EntityDamageByEntityEvent event){
        if(!state.enabled()||!settings.reachCheck()||
                !(event.getDamager() instanceof Player player))return;
        if(player.getWorld()!=event.getEntity().getWorld()||player.isInsideVehicle())return;
        double distance=player.getEyeLocation().distance(event.getEntity().getLocation());
        if(distance>settings.maximumAttackDistance()){
            event.setCancelled(true);
            alert(player.getName(),"Zasięg ataku","dystans "+(int)distance+" bloków");
        }
    }
    @EventHandler(priority=EventPriority.HIGH,ignoreCancelled=true)
    public void place(BlockPlaceEvent event){
        if(!state.enabled()||!settings.placeBurstCheck())return;
        Player player=event.getPlayer();
        if(placeBurst.exceeded(player.getUniqueId(),settings.blocksPerSecond(),
                System.currentTimeMillis())){
            event.setCancelled(true);
            alert(player.getName(),"Szybkie stawianie","limit bloków na sekundę");
        }
    }
    // Ochrona wydajności niezależna od /ac wylacz.
    @EventHandler(priority=EventPriority.HIGH,ignoreCancelled=true)
    public void redstone(BlockRedstoneEvent event){
        if(!settings.redstoneProtection())return;
        if(redstone.exceeded(chunk(event.getBlock().getLocation()),
                settings.redstoneUpdatesPerChunkSecond(),System.currentTimeMillis()))
            event.setNewCurrent(event.getOldCurrent());
    }
    @EventHandler(priority=EventPriority.HIGH,ignoreCancelled=true)
    public void primed(ExplosionPrimeEvent event){
        if(!settings.explosionProtection()||!(event.getEntity() instanceof TNTPrimed))return;
        if(tnt.exceeded(chunk(event.getEntity().getLocation()),
                settings.tntPerChunkFiveSeconds(),System.currentTimeMillis())){
            event.setCancelled(true);
        }
    }
    @EventHandler(priority=EventPriority.HIGH,ignoreCancelled=true)
    public void spawned(CreatureSpawnEvent event){
        if(!settings.entitySpawnProtection())return;
        switch(event.getSpawnReason()){
            case BREEDING,SPAWNER,SPAWNER_EGG,NATURAL -> {
                if(spawn.exceeded(chunk(event.getLocation()),
                        settings.creatureSpawnsPerChunkFiveSeconds(),
                        System.currentTimeMillis()))event.setCancelled(true);
            }
            default -> {}
        }
    }
    public void cleanup(){
        long now=System.currentTimeMillis();
        if(now-lastCleanup<60000)return;
        lastCleanup=now;
        redstone.cleanup(now);tnt.cleanup(now);spawn.cleanup(now);placeBurst.cleanup(now);
        lastAlerts.entrySet().removeIf(e->now-e.getValue()>60000);
    }
    @Override public void close(){writes.shutdown();}
}

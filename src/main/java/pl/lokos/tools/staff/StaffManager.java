package pl.lokos.tools.staff;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import pl.lokos.tools.config.JsonConfigManager;
import pl.lokos.tools.helpers.Colors;
import pl.lokos.tools.manager.RankManager;
import pl.lokos.tools.security.ToolsAccess;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Jedno źródło stanu vanish i bossbar. Paper API wyłącznie na serwerowym wątku;
 * zapisy stanu w pojedynczej kolejce IO, atomowo, bez kasowania danych graczy.
 */
public final class StaffManager implements Listener,AutoCloseable {
    private record Pair(UUID viewer,UUID target) {}
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final JavaPlugin plugin;
    private final RankManager ranks;
    private final Path directory;
    private final ExecutorService writer=Executors.newSingleThreadExecutor(task->{
        Thread thread=new Thread(task,"Tools-Staff-Zapis");thread.setDaemon(false);return thread;
    });
    private final Set<Pair> hiddenByTools=new HashSet<>();
    private final Map<UUID,TextDisplay> vanishTags=new HashMap<>();
    private final Map<UUID,Long> helpopSent=new HashMap<>();
    private final Set<UUID> barViewers=new HashSet<>();
    private volatile StaffState state;
    private final StaffConfig settings;
    private BossBar bar;
    private StaffState.Broadcast renderedBroadcast;
    private int currentTick;

    public StaffManager(JavaPlugin plugin,RankManager ranks,Path directory) throws IOException {
        this.plugin=plugin;this.ranks=ranks;this.directory=directory;
        JsonConfigManager json=new JsonConfigManager(directory);
        try{
            settings=json.load("StaffTools.json",StaffConfig.class,StaffConfig::new,StaffConfig::validate);
            state=json.load("StaffState.json",StaffState.class,StaffState::new,StaffState::validate);
        }catch(IOException ex){writer.shutdown();throw ex;}
    }
    public StaffConfig settings(){return settings;}
    public StaffState state(){return state;}
    public boolean vanished(Player player){return state.vanished(player.getUniqueId());}
    public boolean see(Player player){return ToolsAccess.allowed(player,ranks,"tools.vanish.see",false);}
    public boolean monitor(Player player){return ToolsAccess.allowed(player,ranks,"tools.vanish.monitor",false);}
    public boolean helpopStaff(Player player){return ToolsAccess.allowed(player,ranks,"tools.helpop.receive",false);}

    public synchronized CompletableFuture<Void> vanish(Player player,boolean enabled){
        UUID id=player.getUniqueId();
        if(state.vanished(id)==enabled)return CompletableFuture.failedFuture(
                new IllegalArgumentException(enabled?"Vanish jest już włączony.":"Vanish jest już wyłączony."));
        state=state.withVanish(id,enabled);
        updateVisibility();
        audit(player,enabled?"włączył vanish":"wyłączył vanish","tools.vanish.monitor");
        return write(state);
    }

    public void audit(Player actor,String action,String node){
        String message=settings.staffAlert().replace("{player}",actor.getName()).replace("{action}",action);
        for(Player viewer:Bukkit.getOnlinePlayers())
            if(ToolsAccess.allowed(viewer,ranks,node,false))viewer.sendMessage(Colors.color(message));
    }

    public boolean helpopAllowed(UUID player,long now) {
        long old=helpopSent.getOrDefault(player,0L);
        if(old>0 && now-old<settings.helpopCooldownSeconds()*1000L)return false;
        helpopSent.put(player,now);
        return true;
    }

    public synchronized CompletableFuture<Void> broadcast(String message,int seconds){
        long now=System.currentTimeMillis();
        if(message==null||message.isBlank()||message.length()>500)
            throw new IllegalArgumentException("Treść ogłoszenia musi mieć 1-500 znaków.");
        if(message.indexOf('\r')>=0||message.indexOf('\n')>=0)
            throw new IllegalArgumentException("Treść ogłoszenia musi być w jednej linii.");
        StaffState.Broadcast b=new StaffState.Broadcast(message,now,now+seconds*1000L);
        b.validate();
        state=state.withBroadcast(b);
        renderBroadcast();
        return write(state);
    }
    public synchronized CompletableFuture<Void> clearBroadcast(){
        if(state.activeBroadcast()==null)return CompletableFuture.failedFuture(
                new IllegalArgumentException("Nie ma aktywnego ogłoszenia."));
        state=state.withBroadcast(null);
        renderBroadcast();
        return write(state);
    }

    private CompletableFuture<Void> write(StaffState snapshot){
        return CompletableFuture.runAsync(()->{
            Path file=directory.resolve("StaffState.json"),tmp=null;
            try{
                tmp=Files.createTempFile(directory,".tools-staff-",".tmp");
                Files.writeString(tmp,GSON.toJson(snapshot)+"\n",StandardCharsets.UTF_8);
                try{Files.move(tmp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
                catch(AtomicMoveNotSupportedException ignored){
                    Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING);
                }
            }catch(IOException problem){
                plugin.getLogger().severe("Nie zapisano StaffState.json: "+problem.getMessage());
                throw new CompletionException(problem);
            }finally{if(tmp!=null)try{Files.deleteIfExists(tmp);}catch(IOException ignored){}}
        },writer);
    }

    /** Wcześniej ukryte encje odtwarzamy tylko, jeśli ukryło je Tools. */
    public void updateVisibility(){
        for(Player target:Bukkit.getOnlinePlayers()){
            boolean invisible=vanished(target);
            for(Player viewer:Bukkit.getOnlinePlayers()){
                if(target.equals(viewer))continue;
                Pair pair=new Pair(viewer.getUniqueId(),target.getUniqueId());
                if(invisible && !see(viewer)){
                    if(hiddenByTools.add(pair))viewer.hidePlayer(plugin,target);
                }else if(hiddenByTools.remove(pair)){
                    viewer.showPlayer(plugin,target);
                }
            }
            updateTag(target);
        }
    }
    private void updateTag(Player target){
        UUID id=target.getUniqueId();
        TextDisplay display=vanishTags.get(id);
        if(!vanished(target)){
            if(display!=null){display.remove();vanishTags.remove(id);}
            return;
        }
        if(display==null||!display.isValid()||display.getWorld()!=target.getWorld()){
            if(display!=null)display.remove();
            display=target.getWorld().spawn(tagLocation(target),TextDisplay.class,tag->{
                tag.setVisibleByDefault(false);
                tag.setBillboard(Display.Billboard.CENTER);
                tag.setPersistent(false);
                tag.setSeeThrough(true);
                tag.setShadowed(false);
                tag.setDefaultBackground(false);
            });
            vanishTags.put(id,display);
        }
        display.text(Colors.color(settings.vanishTag()));
        for(Player viewer:Bukkit.getOnlinePlayers()){
            if(viewer.equals(target)||see(viewer))viewer.showEntity(plugin,display);
            else viewer.hideEntity(plugin,display);
        }
    }
    private static Location tagLocation(Player p){
        return p.getLocation().add(0,p.isSneaking()?2.55:3.05,0);
    }

    public void tick(){
        currentTick++;
        if(currentTick%4==0){
            for(Player p:Bukkit.getOnlinePlayers()){
                TextDisplay tag=vanishTags.get(p.getUniqueId());
                if(tag!=null && tag.isValid()){
                    Location target=tagLocation(p);
                    if(tag.getWorld()!=target.getWorld()){updateTag(p);continue;}
                    if(tag.getLocation().distanceSquared(target)>0.005)tag.teleport(target);
                }
            }
        }
        if(currentTick%5!=0)return;
        updateVisibility();
        renderBroadcast();
    }

    private void renderBroadcast(){
        long now=System.currentTimeMillis();
        StaffState.Broadcast active=state.activeBroadcast();
        if(active!=null && !active.active(now)){
            state=state.withBroadcast(null);
            write(state);
            active=null;
        }
        if(active==null){
            if(bar!=null){
                for(Player viewer:Bukkit.getOnlinePlayers())viewer.hideBossBar(bar);
                bar=null;renderedBroadcast=null;barViewers.clear();
            }
            return;
        }
        if(!active.equals(renderedBroadcast)||bar==null){
            if(bar!=null)for(Player viewer:Bukkit.getOnlinePlayers())viewer.hideBossBar(bar);
            bar=BossBar.bossBar(Colors.color(settings.broadcastTitle().replace("{message}",active.message())),
                    1f,BossBar.Color.YELLOW,BossBar.Overlay.PROGRESS);
            renderedBroadcast=active;
            barViewers.clear();
        }
        double duration=(double)(active.untilMillis()-active.startedAt());
        float progress=(float)Math.max(0.0,Math.min(1.0,(active.untilMillis()-now)/duration));
        bar.progress(progress);
        for(Player viewer:Bukkit.getOnlinePlayers())
            if(barViewers.add(viewer.getUniqueId()))viewer.showBossBar(bar);
    }

    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void monsterTargets(EntityTargetLivingEntityEvent event){
        if(event.getTarget() instanceof Player player && vanished(player))
            event.setCancelled(true);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void pickup(EntityPickupItemEvent event){
        if(event.getEntity() instanceof Player player && vanished(player))
            event.setCancelled(true);
    }
    @EventHandler(priority=EventPriority.HIGHEST)
    public void join(PlayerJoinEvent event){
        if(vanished(event.getPlayer()))event.joinMessage(null);
        // HIGHEST po systemie rang: brak ujawniania nicku przy join.
        updateVisibility();
        renderBroadcast();
    }
    @EventHandler public void world(PlayerChangedWorldEvent event){updateVisibility();}
    @EventHandler public void quit(PlayerQuitEvent event){
        UUID id=event.getPlayer().getUniqueId();
        TextDisplay tag=vanishTags.remove(id);
        if(tag!=null)tag.remove();
        hiddenByTools.removeIf(pair->pair.viewer().equals(id)||pair.target().equals(id));
        barViewers.remove(id);
        helpopSent.remove(id);
        // vanish pozostaje w StaffState.json i automatycznie wraca po połączeniu.
    }

    @Override public void close(){
        for(Player viewer:Bukkit.getOnlinePlayers()){
            if(bar!=null)viewer.hideBossBar(bar);
            for(Player target:Bukkit.getOnlinePlayers()){
                if(!viewer.equals(target)&&hiddenByTools.contains(new Pair(viewer.getUniqueId(),target.getUniqueId())))
                    viewer.showPlayer(plugin,target);
            }
        }
        for(TextDisplay t:vanishTags.values())t.remove();
        vanishTags.clear();hiddenByTools.clear();barViewers.clear();
        writer.shutdown();
    }
}

package pl.lokos.tools.border;

import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;
import pl.lokos.tools.config.JsonConfigManager;
import pl.lokos.tools.events.StateFile;
import pl.lokos.tools.helpers.Colors;
import pl.lokos.tools.manager.BossBarHub;
import pl.lokos.tools.manager.RegionManager;
import pl.lokos.tools.region.Region;

import java.io.IOException;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Granica 1500x1500 od spawnu, bez pomniejszania już istniejących działek.
 * Aktualizacja dziennej aktywności raz na minutę, nie w PlayerMoveEvent.
 */
public final class BorderManager implements Listener,AutoCloseable {
    private final JavaPlugin plugin;
    private final RegionManager regions;
    private final BossBarHub bars;
    private final BorderConfig config;
    private final StateFile<BorderState> storage;
    private final Map<UUID,Long> lastActive=new HashMap<>();
    private BossBar bar;
    private boolean initialized;
    public BorderManager(JavaPlugin plugin,RegionManager regions,BossBarHub bars,Path folder) throws IOException{
        this.plugin=plugin;this.regions=regions;this.bars=bars;
        config=new JsonConfigManager(folder).load("WorldBorder.json",BorderConfig.class,
                BorderConfig::new,BorderConfig::validate);
        storage=new StateFile<>(folder,"WorldBorderState.json",BorderState.class,
                BorderState::new,BorderState::validate);
    }
    public BorderConfig config(){return config;}
    public BorderState state(){return storage.get();}
    public boolean ready(){return initialized;}
    private World world(){
        BorderState state=state();
        if(state.world()!=null)try{
            World found=Bukkit.getWorld(UUID.fromString(state.world()));
            if(found!=null)return found;
        }catch(IllegalArgumentException ignored){}
        if(regions!=null && regions.ready() && regions.mainSpawn()!=null){
            World found=Bukkit.getWorld(regions.mainSpawn().world());
            if(found!=null)return found;
        }
        return Bukkit.getWorlds().stream().filter(w->w.getEnvironment()==World.Environment.NORMAL)
                .findFirst().orElse(null);
    }
    private Location initialCenter(World world){
        if(regions!=null&&regions.ready()&&regions.mainSpawn()!=null){
            var spawn=regions.spawnOf(regions.mainSpawn());
            if(spawn!=null&&spawn.getWorld().equals(world))return spawn;
        }
        return world.getSpawnLocation();
    }
    /** Ochrona istniejących regionów, nawet gdy admin rozciągnął je poza 1500. */
    public static int requiredDiameter(double centerX,double centerZ,Collection<Region> regions,UUID world){
        double radius=750;
        for(Region region:regions){
            if(!region.world().equals(world))continue;
            radius=Math.max(radius,Math.abs(region.minX()-centerX)+8);
            radius=Math.max(radius,Math.abs(region.maxX()-centerX)+8);
            radius=Math.max(radius,Math.abs(region.minZ()-centerZ)+8);
            radius=Math.max(radius,Math.abs(region.maxZ()-centerZ)+8);
        }
        return (int)Math.ceil(radius*2);
    }
    private void init(){
        if(initialized||!config.enabled())return;
        if(regions!=null&&!regions.ready())return;
        World world=world();if(world==null)return;
        BorderState state=state();
        if(state.world()==null){
            Location center=initialCenter(world);
            int diameter=requiredDiameter(center.getX(),center.getZ(),
                    regions==null?List.of():regions.index().all().values(),world.getUID());
            diameter=Math.max(diameter,config.initialDiameter());
            // Zachowaj niestandardowy stary border; domyślne 60 mln bloków
            // nie powinno blokować ustawienia granicy startowej.
            double preexisting=world.getWorldBorder().getSize();
            if(preexisting<59999900d)diameter=Math.max(diameter,(int)Math.ceil(preexisting));
            long now=System.currentTimeMillis();
            storage.update(old->old.started(world.getUID(),center.getX(),center.getZ(),diameter,now));
            state=storage.get();
        }
        // Zachowaj całą mapę istniejących działek; nie zmniejszaj borderu przy aktualizacji.
        int required=requiredDiameter(state.centerX(),state.centerZ(),
                regions==null?List.of():regions.index().all().values(),world.getUID());
        if(state.diameter()<required){
            int diameter=required;
            storage.update(old->new BorderState(old.world(),old.centerX(),old.centerZ(),
                    diameter,old.cycleStarted(),old.lastExpansion(),old.paused(),old.dailyMinutes()));
            state=storage.get();
            plugin.getLogger().warning("Granica została powiększona, aby nie odcinać istniejących regionów.");
        }
        var border=world.getWorldBorder();
        border.setCenter(state.centerX(),state.centerZ());
        border.setSize(state.diameter());
        initialized=true;
        for(Player player:Bukkit.getOnlinePlayers())
            lastActive.put(player.getUniqueId(),System.currentTimeMillis());
        plugin.getLogger().info("Tools WorldBorder: "+state.diameter()+"x"+state.diameter());
    }
    @EventHandler public void join(PlayerJoinEvent event){
        lastActive.put(event.getPlayer().getUniqueId(),System.currentTimeMillis());
    }
    @EventHandler public void leave(PlayerQuitEvent event){
        lastActive.remove(event.getPlayer().getUniqueId());
    }
    @EventHandler(ignoreCancelled=true)
    public void movement(PlayerMoveEvent event){
        if(!event.hasChangedPosition())return;
        lastActive.put(event.getPlayer().getUniqueId(),System.currentTimeMillis());
    }
    @EventHandler(ignoreCancelled=true)
    public void interaction(PlayerInteractEvent event){
        lastActive.put(event.getPlayer().getUniqueId(),System.currentTimeMillis());
    }
    /** Wywoływane raz na minutę; nie przechowuje lokacji podczas częstych move eventów. */
    public void tick(){
        init();
        if(!initialized)return;
        long now=System.currentTimeMillis();
        if(!state().paused()){
            String day=LocalDate.now(ZoneOffset.UTC).toString();
            for(Player p:Bukkit.getOnlinePlayers()){
                if(p.getGameMode()==GameMode.SPECTATOR)continue;
                long activity=lastActive.getOrDefault(p.getUniqueId(),0L);
                if(now-activity>config.afkAfterMinutes()*60000L)continue;
                String key=day+":"+p.getUniqueId();
                storage.update(old->old.increment(key,config.dailyPlayerCapMinutes()));
            }
            BorderState current=state();
            long cycleMs=config.cycleDays()*86400000L;
            long fallbackMs=config.fallbackDays()*86400000L;
            long elapsed=now-current.cycleStarted();
            if(elapsed>=cycleMs&&current.totalMinutes()>=config.requiredActiveMinutes()
                    ||elapsed>=fallbackMs){
                int step=elapsed>=fallbackMs&&current.totalMinutes()<config.requiredActiveMinutes()
                        ?config.fallbackExpansionDiameter():config.expansionDiameter();
                expandInternal(step);
            }
        }
        render();
        if(state().dailyMinutes().size()>90000)
            plugin.getLogger().warning("WorldBorderState.json zbliża się do limitu aktywności.");
    }
    private void expandInternal(int additional){
        BorderState old=state();
        if(old.diameter()>=config.maxDiameter())return;
        int next=Math.min(config.maxDiameter(),old.diameter()+additional);
        storage.update(s->s.expanded(next,System.currentTimeMillis()));
        World world=world();
        if(world!=null)world.getWorldBorder().setSize(next,60);
        plugin.getLogger().info("Rozszerzono mapę do "+next+" x "+next+" bloków.");
    }
    public void expand(int additional){
        if(!ready())throw new IllegalArgumentException("Granica jeszcze się wczytuje.");
        if(additional<1||additional>10000)throw new IllegalArgumentException("Dodatkowy rozmiar: 1-10000.");
        expandInternal(additional);
    }
    public CompletableFuture<Void> pause(boolean paused){
        if(!ready())throw new IllegalArgumentException("Granica jeszcze się wczytuje.");
        if(state().paused()==paused)throw new IllegalArgumentException(
                paused?"Rozrost mapy jest już wstrzymany.":"Rozrost mapy już działa.");
        return storage.update(old->old.withPause(paused));
    }
    public String status(){
        BorderState s=state();
        return "Mapa: "+s.diameter()+"×"+s.diameter()+" | Aktywność: "+
                s.totalMinutes()+"/"+config.requiredActiveMinutes()+" min | "+
                (s.paused()?"wstrzymana":"aktywna");
    }
    private void render(){
        if(!config.showProgressBossbar()||state().diameter()>=config.maxDiameter()){
            if(bar!=null){bar=null;bars.setGlobal(BossBarHub.Slot.BORDER,null,null);}
            return;
        }
        if(bar==null){
            bar=BossBar.bossBar(net.kyori.adventure.text.Component.empty(),0f,
                    BossBar.Color.BLUE,BossBar.Overlay.PROGRESS);
            World w=world();
            bars.setGlobal(BossBarHub.Slot.BORDER,bar,w==null?null:w.getUID());
        }
        double fraction=Math.min(1d,state().totalMinutes()/(double)config.requiredActiveMinutes());
        long remaining=Math.max(0,config.cycleDays()*86400000L-
                (System.currentTimeMillis()-state().cycleStarted()));
        long days=(remaining+86400000L-1)/86400000L;
        bar.name(Colors.color(config.bar().replace("{progress}",String.valueOf((int)(fraction*100)))
                .replace("{days}",Long.toString(days))));
        bar.progress((float)fraction);
    }
    @Override public void close(){
        bars.setGlobal(BossBarHub.Slot.BORDER,null,null);
        storage.close();
    }
}

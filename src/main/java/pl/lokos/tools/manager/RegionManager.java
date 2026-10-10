package pl.lokos.tools.manager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import pl.lokos.tools.config.DefinitionFiles;
import pl.lokos.tools.config.RegionsFile;
import pl.lokos.tools.database.RegionRepository;
import pl.lokos.tools.region.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.UnaryOperator;
import java.util.logging.Level;

/** Regions.json jest jedynym źródłem regionów, MySQL przechowuje dane graczy. */
public final class RegionManager {
    private final JavaPlugin plugin;
    private final RegionRepository legacy;
    private final RankManager ranks;
    private final DefinitionFiles definitions;
    private volatile RegionIndex index=RegionIndex.empty();
    private volatile String mainSpawn;
    private volatile boolean loaded;
    private boolean closing;
    private CompletableFuture<Void> queue=CompletableFuture.completedFuture(null);
    private CompletableFuture<Void> firstLoad;

    public RegionManager(JavaPlugin plugin,RegionRepository legacy,RankManager ranks,DefinitionFiles definitions) {
        this.plugin=plugin;this.legacy=legacy;this.ranks=ranks;this.definitions=definitions;
    }
    public synchronized CompletableFuture<Void> start() {
        if(firstLoad!=null)return firstLoad;
        firstLoad=(definitions.importRegions() ? legacy.load().thenAccept(data -> {
            if(!definitions.regions().regions().isEmpty()) {
                definitions.saveRegions(RegionsFile.from(definitions.regions().regions(),
                        definitions.regions().mainSpawn(),definitions.regions().settings()));
                return;
            }
            // Dawny "_ochrona" był błędnym podregionem wewnątrz spawnu.
            // Jest teraz automatycznym buforem poza granicą; nie importujemy artefaktu.
            Set<String> legacyHalo=new HashSet<>();
            for(Region region:data.regions())
                if(region.parent()!=null && region.name().equals(region.parent()+"_ochrona"))
                    legacyHalo.add(region.name());
            List<Region> imported=new ArrayList<>();
            for(Region region:data.regions())
                if(!legacyHalo.contains(region.name()) && !legacyHalo.contains(region.parent()))
                    imported.add(region);
            String spawn=data.mainSpawn();
            if(spawn!=null && new RegionIndex(imported).byName(spawn)==null)spawn=null;
            definitions.saveRegions(RegionsFile.from(imported,spawn,definitions.regions().settings()));
            plugin.getLogger().info("Przeniesiono "+imported.size()+" regionów do Regions.json.");
        }).exceptionally(error -> {
            // Ochrona nie może znikać tylko dlatego, że dawna baza SQL jest niedostępna.
            // Jeżeli Regions.json zawiera regiony, wczytujemy bezpiecznie lokalne definicje.
            if(definitions.regions().regions().isEmpty())
                throw new java.util.concurrent.CompletionException(error);
            plugin.getLogger().log(Level.WARNING,
                    "Nie można zaimportować dawnych regionów SQL. Używam istniejącego Regions.json.", error);
            return null;
        }) : CompletableFuture.<Void>completedFuture(null)).thenCompose(unused->refresh());
        queue=firstLoad.handle((v,e)->null);
        firstLoad.exceptionally(error->{plugin.getLogger().log(Level.SEVERE,"Nie załadowano Regions.json",error);return null;});
        return firstLoad;
    }
    public RankManager ranks(){return ranks;}
    public boolean ready(){return loaded;}
    public RegionIndex index(){return index;}
    public Region at(Location at) {
        if(at==null||at.getWorld()==null)return null;
        return index.at(at.getWorld().getUID(),at.getBlockX(),at.getBlockZ());
    }
    public Region mainSpawn(){return mainSpawn==null?null:index.byName(mainSpawn);}
    public Region visibleAt(Location location) {
        Region region=at(location);
        return region!=null?region:inHalo(location)?mainSpawn():null;
    }
    /** Zewnetrzny pas ochronny - NIGDY nie jest regionem ani lokalizacja w GUI. */
    public boolean inHalo(Location loc) {
        Region spawn=mainSpawn();
        if(spawn==null||loc==null||loc.getWorld()==null||!spawn.world().equals(loc.getWorld().getUID()))return false;
        int amount=definitions.regions().settings().spawnProtectionOutside();
        if(amount<=0)return false;
        return RegionHalo.contains(spawn,loc.getWorld().getUID(),loc.getBlockX(),loc.getBlockZ(),amount);
    }
    public boolean protectedLocation(Location l,RegionFlag flag) {
        Region r=at(l);
        return inHalo(l) || (r!=null && !index.enabled(r,flag));
    }
    public CompletableFuture<Void> refresh() {
        RegionsFile file=definitions.regions();
        RegionIndex next=new RegionIndex(file.regions());
        CompletableFuture<Void> result=new CompletableFuture<>();
        if(closing||!plugin.isEnabled()) {
            result.completeExceptionally(new IllegalStateException("Plugin wyłączony"));return result;
        }
        try{
            plugin.getServer().getScheduler().runTask(plugin,()->{
                boolean first=!loaded;
                index=next;mainSpawn=file.mainSpawn();loaded=true;
                if(first){
                    plugin.getLogger().info("Ochrona regionów aktywna. Wczytano: "+index.all().size()
                            + " | Główny spawn: "+(mainSpawn==null?"nieustawiony":mainSpawn));
                    if(index.all().isEmpty())plugin.getLogger().warning(
                            "Regions.json nie zawiera regionów. Sprawdź import lub utwórz nowy region.");
                    for(Region r:index.all().values()) {
                        if(Bukkit.getWorld(r.world())==null)plugin.getLogger().warning(
                                "Region '"+r.name()+"' należy do niezaładowanego świata UUID="+r.world());
                    }
                }
                result.complete(null);
            });
        }catch(RuntimeException e){result.completeExceptionally(e);}
        return result;
    }
    private synchronized CompletableFuture<Void> change(UnaryOperator<RegionsFile> edit) {
        CompletableFuture<Void> op=queue.handle((v,e)->null)
                .thenCompose(v->CompletableFuture.runAsync(()->{
                    if(closing)throw new IllegalStateException("Plugin wyłączony");
                    RegionsFile next=edit.apply(definitions.regions());
                    definitions.saveRegions(next);
                })).thenCompose(v->refresh());
        queue=op.handle((v,e)->null);
        return op;
    }
    private RegionsFile modified(RegionsFile file,List<Region> changed,String spawn) {
        return RegionsFile.from(changed,spawn,file.settings());
    }
    public CompletableFuture<Void> create(Region r) {
        return change(file->{
            new RegionIndex(file.regions()).validateNew(r);
            Region spawn=file.mainSpawn()==null?null:new RegionIndex(file.regions()).byName(file.mainSpawn());
            int radius=file.settings().spawnProtectionOutside();
            if(r.parent()==null && spawn!=null && radius>0 && r.world().equals(spawn.world())){
                long minX=(long)spawn.minX()-radius,maxX=(long)spawn.maxX()+radius;
                long minZ=(long)spawn.minZ()-radius,maxZ=(long)spawn.maxZ()+radius;
                if(r.minX()<=maxX && r.maxX()>=minX && r.minZ()<=maxZ && r.maxZ()>=minZ)
                    throw new IllegalArgumentException("Region nachodzi na zewnętrzną ochronę spawnu.");
            }

            List<Region> all=new ArrayList<>(file.regions());all.add(r);
            return modified(file,all,file.mainSpawn());
        });
    }
    public CompletableFuture<Void> remove(String name) {
        return change(file->{
            RegionIndex old=new RegionIndex(file.regions());
            if(old.byName(name)==null)throw new IllegalArgumentException("Nie ma takiego regionu.");
            Set<String> deleted=new HashSet<>(Set.of(name));
            boolean grew;
            do{
                grew=false;
                for(Region region:file.regions())if(region.parent()!=null&&deleted.contains(region.parent()))
                    grew|=deleted.add(region.name());
            }while(grew);
            List<Region> kept=file.regions().stream().filter(region->!deleted.contains(region.name())).toList();
            String spawn=deleted.contains(file.mainSpawn())?null:file.mainSpawn();
            return modified(file,kept,spawn);
        });
    }
    private CompletableFuture<Void> edit(String name,UnaryOperator<Region> edit) {
        return change(file->{
            List<Region> all=new ArrayList<>();
            boolean found=false;
            for(Region r:file.regions()){
                if(r.name().equals(name)){all.add(edit.apply(r));found=true;}
                else all.add(r);
            }
            if(!found)throw new IllegalArgumentException("Nie ma takiego regionu.");
            return modified(file,all,file.mainSpawn());
        });
    }
    public CompletableFuture<Void> flag(String name,RegionFlag f,Boolean state) {
        return edit(name,r->{
            Map<RegionFlag,Boolean> flags=new EnumMap<>(RegionFlag.class);
            flags.putAll(r.flags());
            if(state==null)flags.remove(f);else flags.put(f,state);
            return new Region(r.name(),r.world(),r.minX(),r.maxX(),r.minZ(),r.maxZ(),
                    r.parent(),r.entryRank(),flags,r.spawn());
        });
    }
    public CompletableFuture<Void> entryRank(String name,String rank) {
        return edit(name,r->new Region(r.name(),r.world(),r.minX(),r.maxX(),r.minZ(),r.maxZ(),
                r.parent(),rank,r.flags(),r.spawn()));
    }
    public CompletableFuture<Void> setSpawn(String name,Region.Spawn spawn) {
        return change(file->{
            List<Region> all=new ArrayList<>();boolean found=false;
            for(Region r:file.regions()){
                if(r.name().equals(name)){
                    all.add(new Region(r.name(),r.world(),r.minX(),r.maxX(),r.minZ(),r.maxZ(),
                            r.parent(),r.entryRank(),r.flags(),spawn));
                    found=true;
                }else all.add(r);
            }
            if(!found)throw new IllegalArgumentException("Nie ma takiego regionu.");
            return modified(file,all,name);
        });
    }
    public record ProtectionStatus(boolean ready, int regions, String region,
                                   boolean buildingAllowed, boolean breakingAllowed) {}

    public ProtectionStatus protectionStatus(Player p) {
        Region r=at(p.getLocation());
        return new ProtectionStatus(ready(),index.all().size(),r==null?null:r.name(),
                r!=null && index.enabled(r,RegionFlag.BUILD),
                r!=null && index.enabled(r,RegionFlag.BREAK));
    }

    public boolean allowed(Player player,Region region,RegionFlag flag) {
        return region==null||index.enabled(region,flag);
    }
    public boolean canEnter(Player player,Region region) {
        if(region==null)return true;
        RankSnapshot.Rank rank=ranks==null?null:ranks.snapshot().forPlayer(player.getUniqueId());
        Set<String> seen=new HashSet<>();
        while(region!=null&&seen.add(region.name())){
            if(region.entryRank()!=null){
                RankSnapshot.Rank need=ranks.snapshot().ranks().get(region.entryRank());
                if(need==null||need.position()==null||rank==null||rank.position()==null
                        ||rank.position()>need.position())return false;
            }
            region=region.parent()==null?null:index.byName(region.parent());
        }
        return true;
    }
    public Location spawnOf(Region r){
        if(r==null||r.spawn()==null)return null;
        var world=Bukkit.getWorld(r.world());
        if(world==null)return null;
        Region.Spawn s=r.spawn();
        return new Location(world,s.x(),s.y(),s.z(),s.yaw(),s.pitch());
    }
    public void shutdown(){closing=true;}
}

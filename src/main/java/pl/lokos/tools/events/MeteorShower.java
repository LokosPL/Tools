package pl.lokos.tools.events;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import pl.lokos.tools.helpers.Colors;
import pl.lokos.tools.helpers.Messages;
import pl.lokos.tools.manager.RegionManager;
import pl.lokos.tools.region.RegionFlag;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Bezpieczne meteoryty-wizualizacje. Nigdy nie stawia/niszczy bloków,
 * nie wywołuje wybuchów i nie wymusza ładowania chunków.
 * Każdy meteoryt znika po zebraniu, zakończeniu eventu lub unloadzie chunka.
 */
public final class MeteorShower implements Listener,AutoCloseable {
    private record Node(UUID world,int x,int y,int z,long expiresAt,
                        BlockDisplay block,TextDisplay label,Interaction hitbox) {}
    public record LocationHint(String world,int x,int y,int z,int secondsLeft) {}

    private final JavaPlugin plugin;
    private final EventManager events;
    private final EventConfig config;
    private final RegionManager regions;
    private final Map<UUID,Node> nodes=new LinkedHashMap<>();
    private long sessionStart;
    private long nextSpawnAt;

    public MeteorShower(JavaPlugin plugin,EventManager events,
                        EventConfig config,RegionManager regions){
        this.plugin=plugin;this.events=events;this.config=config;this.regions=regions;
    }
    public int activeCount(){return nodes.size();}
    public List<LocationHint> hints(){
        long now=System.currentTimeMillis();
        List<LocationHint> result=new ArrayList<>();
        for(Node node:nodes.values()){
            World world=Bukkit.getWorld(node.world());
            if(world!=null)
                result.add(new LocationHint(world.getName(),node.x(),node.y(),node.z(),
                        (int)Math.max(0,(node.expiresAt()-now+999)/1000)));
        }
        return List.copyOf(result);
    }
    public void sendHints(org.bukkit.command.CommandSender sender){
        if(events.active()!=EventType.METEORY){
            Messages.info(sender,"Deszcz meteorów obecnie nie trwa.");return;
        }
        List<LocationHint> list=hints();
        if(list.isEmpty()){
            Messages.info(sender,"&#A8A8B7Brak aktywnych meteorytów. Nowe pojawią się podczas eventu.");return;
        }
        Messages.title(sender,"AKTYWNE METEORYTY");
        for(LocationHint hint:list)
            Messages.info(sender,"&#FFD166✦ &#70D6E8"+hint.world()+" &#A8A8B7» X: &#FFD166"
                    +hint.x()+" &#A8A8B7Y: &#FFD166"+hint.y()+" &#A8A8B7Z: &#FFD166"
                    +hint.z()+" &#A8A8B7| pozostało "+hint.secondsLeft()+"s");
    }

    public void tick(){
        if(!config.meteorEnabled()||events.active()!=EventType.METEORY||regions==null||!regions.ready()){
            if(!nodes.isEmpty())clear();
            sessionStart=0;nextSpawnAt=0;
            return;
        }
        long now=System.currentTimeMillis(),started=events.state().startedAt();
        if(sessionStart!=started){
            clear();
            sessionStart=started;
            nextSpawnAt=now+5000L;
        }
        for(var iterator=nodes.entrySet().iterator();iterator.hasNext();){
            var entry=iterator.next();
            Node node=entry.getValue();
            World world=Bukkit.getWorld(node.world());
            if(node.expiresAt()<=now||world==null||
                    !world.isChunkLoaded(node.x()>>4,node.z()>>4)||
                    !node.hitbox().isValid()){
                removeVisual(node);
                iterator.remove();
                continue;
            }
            if(config.meteorEffects()){
                Location center=node.hitbox().getLocation().add(0,0.6,0);
                if(world.getPlayers().stream().anyMatch(p->p.getLocation().distanceSquared(center)<48*48)){
                    world.spawnParticle(Particle.END_ROD,center,5,0.55,0.65,0.55,0.015);
                    world.spawnParticle(Particle.FLAME,center,4,0.45,0.4,0.45,0.01);
                }
            }
        }
        if(now<nextSpawnAt||nodes.size()>=config.meteorMaxActive())return;
        // Ograniczenie kolejnych prób przy pełnej mapie / braku załadowanych chunków.
        nextSpawnAt=now+config.meteorSpawnIntervalSeconds()*1000L;
        spawnNearPlayer(now);
    }

    private void spawnNearPlayer(long now){
        List<Player> candidates=new ArrayList<>();
        for(Player player:Bukkit.getOnlinePlayers())
            if(player.getGameMode()==GameMode.SURVIVAL
                    && player.getWorld().getEnvironment()==World.Environment.NORMAL)
                candidates.add(player);
        if(candidates.isEmpty())return;
        Collections.shuffle(candidates);
        int attempts=0;
        for(Player player:candidates){
            if(attempts++>=8)return;
            World world=player.getWorld();
            for(int n=0;n<8;n++){
                int x=player.getLocation().getBlockX()+
                        ThreadLocalRandom.current().nextInt(-config.meteorRadiusBlocks(),
                                config.meteorRadiusBlocks()+1);
                int z=player.getLocation().getBlockZ()+
                        ThreadLocalRandom.current().nextInt(-config.meteorRadiusBlocks(),
                                config.meteorRadiusBlocks()+1);
                int dx=x-player.getLocation().getBlockX(),dz=z-player.getLocation().getBlockZ();
                if(dx*dx+dz*dz<config.meteorMinimumDistanceBlocks()*
                        config.meteorMinimumDistanceBlocks())continue;
                // Nie budzimy niezaładowanych chunków. W praktyce wymagany jest
                // render-distance obejmujący wylosowane współrzędne.
                if(!world.isChunkLoaded(x>>4,z>>4))continue;
                if(!MeteorSpawnPolicy.unprotected(regions.index(),regions.mainSpawn(),
                        world.getUID(),x,z,32))continue;
                int y=world.getHighestBlockYAt(x,z);
                if(y<=world.getMinHeight()+2||y+4>=world.getMaxHeight())continue;
                Location center=new Location(world,x+0.5,y+1.0,z+0.5);
                if(!world.getWorldBorder().isInside(center))continue;
                if(regions.inHalo(center)||regions.protectedLocation(center,RegionFlag.INTERACT))
                    continue;
                Block floor=world.getBlockAt(x,y,z);
                if(!floor.getType().isSolid()||floor.isLiquid() ||
                        !world.getBlockAt(x,y+1,z).getType().isAir() ||
                        !world.getBlockAt(x,y+2,z).getType().isAir())continue;
                place(center,now);
                return;
            }
        }
    }

    private void place(Location location,long now){
        World world=location.getWorld();
        if(world==null)return;
        BlockDisplay visual=world.spawn(location,BlockDisplay.class,d->{
            d.setBlock(Material.MAGMA_BLOCK.createBlockData());
            d.setPersistent(false);
            d.setGlowing(true);
        });
        TextDisplay label=world.spawn(location.clone().add(0,1.8,0),TextDisplay.class,d->{
            d.setPersistent(false);
            d.setBillboard(Display.Billboard.CENTER);
            d.setShadowed(true);
            d.setDefaultBackground(false);
            d.text(Colors.color("&#FF727F☄ METEORYT &#FFD166✦\n&#A8A8B7Prawy klik = nagroda"));
        });
        Interaction hit=world.spawn(location.clone().add(0,0.5,0),Interaction.class,d->{
            d.setPersistent(false);
            d.setInteractionWidth(1.5f);
            d.setInteractionHeight(2.0f);
        });
        nodes.put(hit.getUniqueId(),new Node(world.getUID(),location.getBlockX(),
                location.getBlockY(),location.getBlockZ(),now+config.meteorLifetimeSeconds()*1000L,
                visual,label,hit));
        if(config.meteorAnnouncements())
            Bukkit.broadcast(Colors.color("&#FF727F☄ Spadł meteoryt! &#A8A8B7Świat: &#70D6E8"
                    +world.getName()+" &#A8A8B7X: &#FFD166"+location.getBlockX()
                    +" &#A8A8B7Z: &#FFD166"+location.getBlockZ()
                    +" &#A8A8B7| szczegóły: &#70D6E8/meteory gdzie"));
        world.playSound(location,Sound.ENTITY_GENERIC_EXPLODE,0.8f,0.6f);
        world.spawnParticle(Particle.END_ROD,location.clone().add(0,1,0),35,1.5,1,1.5,0.12);
    }

    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void claim(PlayerInteractEntityEvent event){
        Node node=nodes.get(event.getRightClicked().getUniqueId());
        if(node==null)return;
        Player player=event.getPlayer();
        if(events.active()!=EventType.METEORY||
                player.getGameMode()!=GameMode.SURVIVAL||
                !node.world().equals(player.getWorld().getUID())||
                player.getLocation().distanceSquared(node.hitbox().getLocation())>36||
                !valid(node)){
            Messages.error(player,"Nie można zebrać tego meteorytu.");return;
        }
        // Jeden wątek Paper: usuwamy przed nagrodą, więc kolejne pakiety kliknięć
        // nie mogą wypłacić jej powtórnie.
        nodes.remove(event.getRightClicked().getUniqueId());
        removeVisual(node);
        events.meteorCollected(player,config.meteorKeysPerMeteor());
    }

    private boolean valid(Node node){
        World world=Bukkit.getWorld(node.world());
        if(world==null||!world.isChunkLoaded(node.x()>>4,node.z()>>4)||
                node.expiresAt()<System.currentTimeMillis())return false;
        Location location=new Location(world,node.x()+0.5,node.y()+0.5,node.z()+0.5);
        return regions.ready()
                && MeteorSpawnPolicy.unprotected(regions.index(),regions.mainSpawn(),
                    node.world(),node.x(),node.z(),32)
                && !regions.inHalo(location)
                && !regions.protectedLocation(location,RegionFlag.INTERACT)
                && world.getWorldBorder().isInside(location);
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void chunkUnload(ChunkUnloadEvent event){
        for(var it=nodes.entrySet().iterator();it.hasNext();){
            Node node=it.next().getValue();
            if(node.world().equals(event.getWorld().getUID())
                    && (node.x()>>4)==event.getChunk().getX()
                    && (node.z()>>4)==event.getChunk().getZ()){
                removeVisual(node);it.remove();
            }
        }
    }
    private void removeVisual(Node node){
        if(node.hitbox().isValid())node.hitbox().remove();
        if(node.block().isValid())node.block().remove();
        if(node.label().isValid())node.label().remove();
    }
    public void clear(){
        for(Node node:nodes.values())removeVisual(node);
        nodes.clear();
    }
    @Override public void close(){clear();}
}

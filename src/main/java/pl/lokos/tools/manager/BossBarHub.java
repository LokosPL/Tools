package pl.lokos.tools.manager;

import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.util.*;

/**
 * Jedyny właściciel bossbarów Tools: event zawsze przed walką,
 * ogłoszeniem i granicą. Nie dotyka bossbarów innych pluginów.
 * Nie wysyła pustych wiadomości ani nie ukrywa bez potrzeby tych samych pasków.
 */
public final class BossBarHub {
    public enum Slot { EVENT, COMBAT, BROADCAST, BORDER, AFK }
    private record Global(BossBar bar,UUID world) {}
    private final EnumMap<Slot,Global> globals=new EnumMap<>(Slot.class);
    private final Map<UUID,BossBar> combat=new HashMap<>();
    private final Map<UUID,BossBar> afk=new HashMap<>();
    private final Map<UUID,List<BossBar>> shown=new HashMap<>();

    public void setGlobal(Slot slot,BossBar bar,UUID world){
        if(slot==Slot.COMBAT||slot==Slot.AFK)
            throw new IllegalArgumentException("Ten bossbar jest indywidualny.");
        if(bar==null)globals.remove(slot);
        else globals.put(slot,new Global(bar,world));
        refresh();
    }
    public void combat(UUID player,BossBar bar){
        if(bar==null)combat.remove(player);else combat.put(player,bar);
        refresh();
    }
    /** Personalny bossbar AFK, pokazywany tylko osobom w strefie. */
    public void afk(UUID player,BossBar bar){
        if(bar==null)afk.remove(player);else afk.put(player,bar);
        refresh();
    }
    public void refresh(){
        Set<UUID> online=new HashSet<>();
        for(Player player:Bukkit.getOnlinePlayers()){
            UUID id=player.getUniqueId();online.add(id);
            List<BossBar> next=new ArrayList<>(5);
            for(Slot slot:Slot.values()){
                if(slot==Slot.COMBAT){
                    BossBar personal=combat.get(id);if(personal!=null)next.add(personal);
                }else if(slot==Slot.AFK){
                    BossBar personal=afk.get(id);if(personal!=null)next.add(personal);
                }else{
                    Global global=globals.get(slot);
                    if(global!=null&&(global.world()==null||global.world().equals(player.getWorld().getUID())))
                        next.add(global.bar());
                }
            }
            List<BossBar> previous=shown.getOrDefault(id,List.of());
            if(previous.equals(next))continue;
            for(BossBar bar:previous)player.hideBossBar(bar);
            for(BossBar bar:next)player.showBossBar(bar);
            shown.put(id,List.copyOf(next));
        }
        shown.keySet().removeIf(id->!online.contains(id));
        combat.keySet().removeIf(id->!online.contains(id));
        afk.keySet().removeIf(id->!online.contains(id));
    }
    public void shutdown(){
        for(Player player:Bukkit.getOnlinePlayers())
            for(BossBar bar:shown.getOrDefault(player.getUniqueId(),List.of()))
                player.hideBossBar(bar);
        shown.clear();combat.clear();afk.clear();globals.clear();
    }
}

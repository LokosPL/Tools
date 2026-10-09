package pl.lokos.tools.region;

import org.bukkit.Location;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

/** Dwa punkty wybrane przez moderatora; dane pozostaja tylko w pamieci. */
public final class RegionSelection {
    public record Pair(Location first, Location second) {
        public boolean complete() { return first!=null && second!=null
                && first.getWorld().equals(second.getWorld()); }
    }
    private final Map<UUID,Pair> selections=new HashMap<>();
    public Pair get(UUID uuid) { return selections.getOrDefault(uuid,new Pair(null,null)); }
    public void choose(UUID uuid, boolean first, Location loc) {
        Pair previous=get(uuid);
        selections.put(uuid,first?new Pair(loc.clone(),previous.second()):new Pair(previous.first(),loc.clone()));
    }
    public void clear(UUID uuid) { selections.remove(uuid); }
}

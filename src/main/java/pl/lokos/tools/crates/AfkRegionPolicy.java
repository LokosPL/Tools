package pl.lokos.tools.crates;

import pl.lokos.tools.region.Region;
import pl.lokos.tools.region.RegionIndex;
import java.util.UUID;

/** AFK działa wyłącznie w regionie o dokładnej nazwie afk i jego podregionach. */
public final class AfkRegionPolicy {
    private AfkRegionPolicy() {}
    public static boolean contains(RegionIndex index,UUID world,int x,int z){
        if(index==null||world==null)return false;
        for(Region region:index.all().values())
            if("afk".equalsIgnoreCase(region.name()))
                return region.contains(world,x,z);
        return false;
    }
}

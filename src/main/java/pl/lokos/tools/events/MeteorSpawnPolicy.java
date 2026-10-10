package pl.lokos.tools.events;

import pl.lokos.tools.region.Region;
import pl.lokos.tools.region.RegionIndex;
import java.util.UUID;

/**
 * Wybór wolnej przestrzeni: meteoryty nie mogą pojawiać się na spawnie,
 * wewnątrz jakiegokolwiek regionu Tools ani w pasie ochronnym spawnu.
 * Decyzja bazuje na współrzędnych, bez użycia Bukkit API.
 */
public final class MeteorSpawnPolicy {
    private MeteorSpawnPolicy(){}
    public static boolean unprotected(RegionIndex index,Region spawn,UUID world,
                                       int x,int z,int spawnMargin){
        if(index==null||world==null||spawnMargin<0)return false;
        if(index.at(world,x,z)!=null)return false;
        if(spawn!=null && world.equals(spawn.world())){
            long minX=(long)spawn.minX()-spawnMargin,maxX=(long)spawn.maxX()+spawnMargin;
            long minZ=(long)spawn.minZ()-spawnMargin,maxZ=(long)spawn.maxZ()+spawnMargin;
            if(x>=minX&&x<=maxX&&z>=minZ&&z<=maxZ)return false;
        }
        return true;
    }
}

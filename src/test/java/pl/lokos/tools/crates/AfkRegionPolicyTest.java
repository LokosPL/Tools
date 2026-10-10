package pl.lokos.tools.crates;

import org.junit.jupiter.api.Test;
import pl.lokos.tools.region.Region;
import pl.lokos.tools.region.RegionIndex;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AfkRegionPolicyTest {
    @Test void afkIncludesNestedRegionsButNeverOtherWorldOrSpawn(){
        UUID world=UUID.randomUUID();
        Region afk=new Region("afk",world,0,100,0,100,
                null,null,Map.of(),null);
        Region sub=new Region("minigra",world,10,20,10,20,
                "afk",null,Map.of(),null);
        RegionIndex index=new RegionIndex(List.of(afk,sub));
        assertEquals("minigra",index.at(world,15,15).name());
        assertTrue(AfkRegionPolicy.contains(index,world,15,15));
        assertTrue(AfkRegionPolicy.contains(index,world,0,100));
        assertFalse(AfkRegionPolicy.contains(index,world,-1,15));
        assertFalse(AfkRegionPolicy.contains(index,UUID.randomUUID(),15,15));
        assertFalse(AfkRegionPolicy.contains(RegionIndex.empty(),world,15,15));
    }
}

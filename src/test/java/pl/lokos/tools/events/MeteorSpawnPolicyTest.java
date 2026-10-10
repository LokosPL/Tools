package pl.lokos.tools.events;

import org.junit.jupiter.api.Test;
import pl.lokos.tools.region.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MeteorSpawnPolicyTest {
    @Test void onlyWildOutsideSpawnAndItsHaloIsValid(){
        UUID world=UUID.randomUUID();
        Region spawn=new Region("spawn",world,-20,20,-20,20,
                null,null,Map.of(),null);
        Region plot=new Region("dzialka",world,100,130,100,130,
                null,null,Map.of(),null);
        RegionIndex index=new RegionIndex(List.of(spawn,plot));
        assertFalse(MeteorSpawnPolicy.unprotected(index,spawn,world,1,1,32));
        assertFalse(MeteorSpawnPolicy.unprotected(index,spawn,world,50,0,32));
        assertFalse(MeteorSpawnPolicy.unprotected(index,spawn,world,105,110,32));
        assertTrue(MeteorSpawnPolicy.unprotected(index,spawn,world,70,70,32));
        assertTrue(MeteorSpawnPolicy.unprotected(index,spawn,UUID.randomUUID(),70,70,32));
        assertFalse(MeteorSpawnPolicy.unprotected(null,spawn,world,70,70,32));
        assertFalse(MeteorSpawnPolicy.unprotected(index,spawn,null,70,70,32));
        assertFalse(MeteorSpawnPolicy.unprotected(index,spawn,world,70,70,-1));
    }
    @Test void spawnProtectionDoesNotOverflowAtWorldBoundary(){
        UUID world=UUID.randomUUID();
        Region spawn=new Region("spawn",world,29999990,30000000,0,10,
                null,null,Map.of(),null);
        assertFalse(MeteorSpawnPolicy.unprotected(new RegionIndex(List.of(spawn)),
                spawn,world,29999999,0,1024));
    }
}

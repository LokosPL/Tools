package pl.lokos.tools.region;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class RegionHaloTest {
    private final UUID world=UUID.randomUUID();
    private final Region spawn=new Region("spawn",world,-100,100,-100,100,null,null,Map.of(),null);

    @Test void outsideBufferCoversFiftyBlocksInEachDirectionOnly() {
        assertFalse(RegionHalo.contains(spawn,world,0,0,50)); // środek to spawn, nie bufor
        assertTrue(RegionHalo.contains(spawn,world,101,0,50));
        assertTrue(RegionHalo.contains(spawn,world,150,0,50));
        assertTrue(RegionHalo.contains(spawn,world,-150,0,50));
        assertTrue(RegionHalo.contains(spawn,world,150,150,50));
        assertFalse(RegionHalo.contains(spawn,world,151,0,50));
        assertFalse(RegionHalo.contains(spawn,world,0,0,0));
        assertFalse(RegionHalo.contains(spawn,UUID.randomUUID(),101,0,50));
    }
}

package pl.lokos.tools.border;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.lokos.tools.events.StateFile;
import pl.lokos.tools.manager.RegionManager;
import pl.lokos.tools.region.Region;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class BorderStateTest {
    @TempDir Path directory;
    @Test void initialMapIs1500SquareAndClaimsCannotBeCutOff(){
        UUID world=UUID.randomUUID();
        assertEquals(1500,BorderManager.requiredDiameter(0,0,List.of(),world));
        Region claim=new Region("dzialka",world,1800,1950,-100,100,null,null,Map.of(),null);
        assertTrue(BorderManager.requiredDiameter(0,0,List.of(claim),world)>=3916);
        assertEquals(1500,BorderManager.requiredDiameter(0,0,List.of(claim),UUID.randomUUID()));
    }
    @Test void dailyCapAndRestartDoNotResetProgress() throws Exception {
        UUID world=UUID.randomUUID(),player=UUID.randomUUID();
        String id="2026-10-10:"+player;
        long now=System.currentTimeMillis();
        try(var storage=new StateFile<>(directory,"WorldBorderState.json",BorderState.class,
                BorderState::new,BorderState::validate)){
            storage.update(s->s.started(world,0,0,1500,now)).join();
            storage.update(s->s.increment(id,1)).join();
            storage.update(s->s.increment(id,1)).join();
            assertEquals(1,storage.get().totalMinutes());
            storage.update(s->s.withPause(true)).join();
        }
        try(var stored=new StateFile<>(directory,"WorldBorderState.json",BorderState.class,
                BorderState::new,BorderState::validate)){
            assertEquals(1500,stored.get().diameter());
            assertTrue(stored.get().paused());
            assertEquals(1,stored.get().totalMinutes());
            stored.update(s->s.expanded(2500,System.currentTimeMillis())).join();
            assertEquals(2500,stored.get().diameter());
            assertEquals(0,stored.get().totalMinutes());
        }
    }
}

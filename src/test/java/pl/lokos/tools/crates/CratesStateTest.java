package pl.lokos.tools.crates;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.lokos.tools.events.StateFile;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CratesStateTest {
    @TempDir Path directory;
    @Test void fiveCratesAndKeysUseRecognizedTypes(){
        assertEquals(5,CrateType.values().length);
        CratesConfig config=new CratesConfig();
        config.validate();
        for(CrateType type:CrateType.values())assertFalse(config.pool(type).isEmpty());
        assertNotNull(CrateType.parse("eventowa"));
        assertNull(CrateType.parse("bukkit:op"));
    }
    @Test void weightedRewardsArePureAndRejectBadIndexes(){
        assertEquals("minecraft:iron_ingot",CratesConfig.choose(
                Map.of("minecraft:iron_ingot",2,"minecraft:diamond",1),1));
        assertThrows(IllegalArgumentException.class,()->CratesConfig.choose(
                Map.of("minecraft:diamond",3),3));
    }
    @Test void spawnCratesAndAfkProgressSurviveRestart() throws Exception {
        UUID world=UUID.randomUUID(),uuid=UUID.randomUUID();
        var place=new CratesState.Position(world.toString(),10,64,20,"zwykla");
        try(var file=new StateFile<>(directory,"CratesState.json",CratesState.class,
                CratesState::new,CratesState::validate)){
            file.update(s->s.with(place)).join();
            file.update(s->s.withAfk(Map.of(uuid.toString(),45))).join();
            assertThrows(IllegalArgumentException.class,()->file.get().with(place));
        }
        try(var restored=new StateFile<>(directory,"CratesState.json",CratesState.class,
                CratesState::new,CratesState::validate)){
            assertEquals(place,restored.get().get(place.key()));
            assertEquals(45,restored.get().afkMinutes().get(uuid.toString()));
            restored.update(s->s.without(place.key())).join();
            assertNull(restored.get().get(place.key()));
        }
    }
}

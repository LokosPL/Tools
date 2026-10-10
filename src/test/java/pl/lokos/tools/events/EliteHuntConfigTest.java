package pl.lokos.tools.events;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.lokos.tools.config.JsonConfigManager;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class EliteHuntConfigTest {
    @TempDir Path dir;
    @Test void ninthEventHasDedicatedGoalsAndCommandIdentifier(){
        assertEquals(EventType.LOWY,EventType.parse("lowy"));
        assertEquals("ostrze_lowcy",EventType.LOWY.itemId());
        assertArrayEquals(new int[]{2,6,12},EventChallenges.goals(EventType.LOWY));
        assertEquals(9,EventType.names().size());
    }
    @Test void oldEventConfigurationGainsEliteOptionsWithoutLosingCustomSettings() throws Exception {
        Files.writeString(dir.resolve("Events.json"),
                "{\"defaultDurationMinutes\":120,\"custom\":\"zostaw\"}");
        EventConfig c=new JsonConfigManager(dir).load("Events.json",
                EventConfig.class,EventConfig::new,EventConfig::validate);
        assertEquals(120,c.defaultDurationMinutes());
        assertTrue(c.eliteEnabled());
        assertEquals(2,c.eliteMaxActive());
        assertEquals(1,c.eliteKeysPerKill());
        String persisted=Files.readString(dir.resolve("Events.json"));
        assertTrue(persisted.contains("\"custom\""));
        assertTrue(persisted.contains("\"eliteSpawnIntervalSeconds\""));
    }
    @Test void invalidEliteOptionsAreRejected(){
        for(String raw:new String[]{
                "{\"eliteMaxActive\":0}",
                "{\"eliteMaxActive\":100}",
                "{\"eliteKeysPerKill\":0}",
                "{\"eliteKeysPerKill\":65}",
                "{\"eliteSpawnIntervalSeconds\":0}",
                "{\"eliteLifetimeSeconds\":0}"}){
            EventConfig c=new Gson().fromJson(raw,EventConfig.class);
            assertThrows(IllegalArgumentException.class,c::validate,raw);
        }
    }
}

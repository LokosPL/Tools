package pl.lokos.tools.events;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.lokos.tools.config.JsonConfigManager;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class MeteorConfigTest {
    @TempDir Path dir;
    @Test void oldEventsJsonReceivesNewSettingsWithoutLosingOldKeys() throws Exception{
        Files.writeString(dir.resolve("Events.json"),
                "{\"tokenChance\":0.06,\"custom\":\"zostaw\"}");
        EventConfig config=new JsonConfigManager(dir).load("Events.json",EventConfig.class,
                EventConfig::new,EventConfig::validate);
        assertEquals(0.06,config.tokenChance(),0.000001);
        assertTrue(config.meteorEnabled());
        assertEquals(3,config.meteorMaxActive());
        assertEquals(1,config.meteorKeysPerMeteor());
        String raw=Files.readString(dir.resolve("Events.json"));
        assertTrue(raw.contains("\"custom\""));
        assertTrue(raw.contains("\"meteorSpawnIntervalSeconds\""));
        assertTrue(raw.contains("\"meteorLifetimeSeconds\""));
    }
    @Test void invalidSpawnConfigIsRejected(){
        for(String value:new String[]{
                "{\"meteorMaxActive\":100}",
                "{\"meteorKeysPerMeteor\":0}",
                "{\"meteorRadiusBlocks\":4}",
                "{\"meteorMinimumDistanceBlocks\":150}",
                "{\"meteorLifetimeSeconds\":2}",
                "{\"meteorSpawnIntervalSeconds\":0}"}){
            assertThrows(IllegalArgumentException.class,
                    ()->new Gson().fromJson(value,EventConfig.class).validate(),value);
        }
    }
}

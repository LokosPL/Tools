package pl.lokos.tools.crates;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.lokos.tools.config.JsonConfigManager;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class AfkConfigBonusTest {
    @TempDir Path dir;
    @Test void defaultsRewardStandingStillAndShowBossbar(){
        CratesConfig c=new CratesConfig();
        assertFalse(c.afkRequireActivity());
        assertTrue(c.afkBossbarEnabled());
        assertEquals(60,c.afkKeyMinutes());
        assertEquals(60,c.afkBonusEveryMinutes());
        assertEquals(4,c.afkMaxKeysPerReward());
        assertDoesNotThrow(c::validate);
        assertTrue(c.afkBossbar().contains("{time}"));
    }
    @Test void invalidBonusValuesCannotBeSaved(){
        for(String invalid:new String[]{
                "{\"afkBonusEveryMinutes\":0}",
                "{\"afkBonusEveryMinutes\":2000}",
                "{\"afkMaxKeysPerReward\":0}",
                "{\"afkMaxKeysPerReward\":65}",
                "{\"afkBossbar\":\"brak-placeholderow\"}"}){
            CratesConfig c=new Gson().fromJson(invalid,CratesConfig.class);
            assertThrows(IllegalArgumentException.class,c::validate,invalid);
        }
    }
    @Test void oldJsonKeepsCustomSettingsAndGainsBonusOptions() throws Exception {
        Files.writeString(dir.resolve("Crates.json"),
                "{\"afkKeyMinutes\":90,\"ordinaryKeyChanceFromHostileMob\":0.04,\"extra\":\"zachowaj\"}");
        CratesConfig config=new JsonConfigManager(dir).load(
                "Crates.json",CratesConfig.class,CratesConfig::new,CratesConfig::validate);
        assertEquals(90,config.afkKeyMinutes());
        assertEquals(0.04,config.ordinaryKeyChanceFromHostileMob());
        assertFalse(config.afkRequireActivity());
        assertTrue(config.afkBossbarEnabled());
        String persisted=Files.readString(dir.resolve("Crates.json"));
        assertTrue(persisted.contains("\"extra\""));
        assertTrue(persisted.contains("\"afkBonusEveryMinutes\""));
        assertTrue(persisted.contains("\"afkMaxKeysPerReward\""));
        assertTrue(persisted.contains("\"afkRequireActivity\""));
    }
}

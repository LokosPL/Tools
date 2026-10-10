package pl.lokos.tools.anticheat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.lokos.tools.config.JsonConfigManager;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class AntiCheatConfigPersistenceTest {
    @TempDir Path directory;
    @Test void oldConfigsAndAlertPreferencesNotLostOnRestart() throws Exception {
        JsonConfigManager m=new JsonConfigManager(directory);
        var a=m.load("AntiCheat.json",AntiCheatConfig.class,AntiCheatConfig::new,
                AntiCheatConfig::validate);
        assertTrue(a.redstoneProtection());
        Path f=directory.resolve("AntiCheat.json");
        Files.writeString(f,Files.readString(f).replace("\"blocksPerSecond\": 32","\"blocksPerSecond\": 40"));
        var b=m.load("AntiCheat.json",AntiCheatConfig.class,AntiCheatConfig::new,
                AntiCheatConfig::validate);
        assertEquals(40,b.blocksPerSecond());
        var state=m.load("AntiCheatState.json",AntiCheatState.class,
                AntiCheatState::new,AntiCheatState::validate);
        assertTrue(state.enabled());
    }
}

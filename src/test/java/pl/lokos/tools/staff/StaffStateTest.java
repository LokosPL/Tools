package pl.lokos.tools.staff;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.lokos.tools.config.JsonConfigManager;
import java.nio.file.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class StaffStateTest {
    @TempDir Path folder;

    @Test void persistedVanishRestoresWithoutOverwritingOldConfig() throws Exception{
        var json=new JsonConfigManager(folder);
        var defaults=json.load("StaffTools.json",StaffConfig.class,StaffConfig::new,StaffConfig::validate);
        assertTrue(defaults.vanishTag().contains("VANISH"));
        StaffState initial=json.load("StaffState.json",StaffState.class,StaffState::new,StaffState::validate);
        UUID vanished=UUID.randomUUID();
        StaffState enabled=initial.withVanish(vanished,true);
        assertTrue(enabled.vanished(vanished));
        Files.writeString(folder.resolve("StaffState.json"),new Gson().toJson(enabled));
        StaffState restored=json.load("StaffState.json",StaffState.class,StaffState::new,StaffState::validate);
        assertTrue(restored.vanished(vanished));
        assertFalse(restored.withVanish(vanished,false).vanished(vanished));
        assertFalse(initial.vanished(vanished));
    }
    @Test void broadcastSurvivesReloadAndExpiresWithoutMutatingVanish() throws Exception{
        var json=new JsonConfigManager(folder);
        UUID vanished=UUID.randomUUID();
        long now=System.currentTimeMillis();
        StaffState.Broadcast announcement=new StaffState.Broadcast("Test 1",now,now+86400000);
        var original=new StaffState().withVanish(vanished,true).withBroadcast(announcement);
        assertTrue(announcement.active(now));
        assertFalse(announcement.active(now+86400000));
        json.load("StaffState.json",StaffState.class,StaffState::new,StaffState::validate);
        Files.writeString(folder.resolve("StaffState.json"),new Gson().toJson(original));
        var loaded=json.load("StaffState.json",StaffState.class,StaffState::new,StaffState::validate);
        assertEquals("Test 1",loaded.activeBroadcast().message());
        assertTrue(loaded.vanished(vanished));
        assertTrue(loaded.withBroadcast(null).vanished(vanished));
        assertThrows(IllegalArgumentException.class,()->new StaffState.Broadcast("",10,9).validate());
    }
    @Test void invalidSettingsRejectedBeforePluginBoot(){
        var gson=new Gson();
        assertThrows(IllegalArgumentException.class,()->
                gson.fromJson("{\"helpopCooldownSeconds\":-1}",StaffConfig.class).validate());
        assertThrows(IllegalArgumentException.class,()->
                gson.fromJson("{\"vanishTag\":null}",StaffConfig.class).validate());
    }
}

package pl.lokos.tools.items;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.lokos.tools.config.JsonConfigManager;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class SpecialItemsConfigTest {
    @TempDir Path directory;
    @Test void generatesOnlyOneTestItemAndKeepsCustomLoreAfterRestart() throws Exception {
        var manager=new JsonConfigManager(directory);
        var original=manager.load("SpecialItems.json",SpecialItemsConfig.class,
                SpecialItemsConfig::new,SpecialItemsConfig::validate);
        assertEquals(9,original.items().size());
        assertNotNull(original.get("miecz_duchow"));
        assertNotNull(original.get("buty_zajaczka"));
        var boot=original.get("buty_szybkosci");
        assertEquals("DIAMOND_BOOTS",boot.material());
        assertEquals(2,boot.speedLevel());
        assertEquals(1,boot.jumpLevel());
        assertTrue(boot.lore().stream().anyMatch(s->s.contains("{event}")));
        Path saved=directory.resolve("SpecialItems.json");
        String changed=Files.readString(saved).replace("EVENT TESTOWY","EVENT WIOSENNY");
        Files.writeString(saved,changed);
        var restored=manager.load("SpecialItems.json",SpecialItemsConfig.class,
                SpecialItemsConfig::new,SpecialItemsConfig::validate);
        assertEquals("EVENT WIOSENNY",restored.get("buty_szybkosci").eventName());
        assertTrue(Files.readString(saved).contains("EVENT WIOSENNY"));
    }
    @Test void invalidItemDoesNotOverwriteJson() throws Exception {
        var config=new Gson().fromJson("""
                {"items":{"test":{"eventEnd":"2024-01-01","eventStart":"2025-01-01"}}}
                """,SpecialItemsConfig.class);
        assertThrows(IllegalArgumentException.class,config::validate);
        assertTrue(SpecialItemsConfig.format("{event} {speed}",
                new SpecialItemsConfig().get("buty_szybkosci")).contains("2"));
    }
}

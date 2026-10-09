package pl.lokos.tools.config;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ToolsConfigTest {
    private final Gson gson = new Gson();

    @Test
    void acceptsDefaultConfig() {
        ToolsConfig config = gson.fromJson("{}", ToolsConfig.class);
        assertDoesNotThrow(config::validate);
        assertEquals(30, config.autosaveSeconds());
        assertTrue(config.database().enabled());
        assertEquals("root", config.database().username());
        assertEquals("", config.database().password());
        assertTrue(config.database().createDatabaseIfMissing());
        assertTrue(config.commands().tools().enabled());
        assertEquals("tools.admin", config.commands().tools().permission());
        assertEquals(List.of(), config.commands().tools().aliases());
    }

    @Test
    void acceptsLegacyConfigWithoutCommandsSection() {
        ToolsConfig config = gson.fromJson("{\"autosaveSeconds\":60,\"database\":{\"enabled\":false}}", ToolsConfig.class);
        assertDoesNotThrow(config::validate);
        assertTrue(config.commands().tools().enabled());
    }

    @Test
    void supportsConfiguredCommandSettings() {
        ToolsConfig config = gson.fromJson("""
                {"commands":{"tools":{"enabled":false,"description":"My tools",
                "permission":"mytools.manage","aliases":["narzedzia","tc"]}}}
                """, ToolsConfig.class);
        assertDoesNotThrow(config::validate);
        assertFalse(config.commands().tools().enabled());
        assertEquals(List.of("narzedzia", "tc"), config.commands().tools().aliases());
    }

    @Test
    void rejectsDuplicateAliasesIgnoringCase() {
        ToolsConfig config = gson.fromJson("""
                {"commands":{"tools":{"aliases":["Test","test"]}}}
                """, ToolsConfig.class);
        assertThrows(IllegalArgumentException.class, config::validate);
    }

    @Test
    void rejectsInvalidCommandPermission() {
        ToolsConfig config = gson.fromJson("""
                {"commands":{"tools":{"permission":"bad permission"}}}
                """, ToolsConfig.class);
        assertThrows(IllegalArgumentException.class, config::validate);
    }

    @Test
    void rejectsTooFrequentAutosave() {
        ToolsConfig config = gson.fromJson("{\"autosaveSeconds\":1}", ToolsConfig.class);
        assertThrows(IllegalArgumentException.class, config::validate);
    }
}

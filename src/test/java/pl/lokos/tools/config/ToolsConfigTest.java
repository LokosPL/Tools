package pl.lokos.tools.config;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ToolsConfigTest {
    @Test
    void acceptsDefaultConfig() {
        ToolsConfig config = new Gson().fromJson("{}", ToolsConfig.class);
        assertDoesNotThrow(config::validate);
        assertEquals(30, config.autosaveSeconds());
        assertFalse(config.database().enabled());
    }

    @Test
    void rejectsTooFrequentAutosave() {
        ToolsConfig config = new Gson().fromJson("{\"autosaveSeconds\":1}", ToolsConfig.class);
        assertThrows(IllegalArgumentException.class, config::validate);
    }
}

package pl.lokos.tools.config;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class JsonConfigManagerTest {
    @TempDir
    Path tempDir;

    private ToolsConfig load() throws IOException {
        return new JsonConfigManager(tempDir).load(
                "config.json", ToolsConfig.class, ToolsConfig::new, ToolsConfig::validate);
    }

    @Test
    void generatesJsonFromJavaWithoutResourceTemplate() throws IOException {
        ToolsConfig result = load();
        Path file = tempDir.resolve("config.json");

        assertTrue(Files.exists(file));
        assertEquals(30, result.autosaveSeconds());
        var json = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        assertEquals(30, json.get("autosaveSeconds").getAsInt());
        assertTrue(json.getAsJsonObject("commands").getAsJsonObject("tools").get("enabled").getAsBoolean());
        assertEquals("tools", json.getAsJsonObject("database").get("database").getAsString());
    }

    @Test
    void keepsCustomSettingsAndAddsMissingKeysOnNextStart() throws IOException {
        Path file = tempDir.resolve("config.json");
        Files.writeString(file, """
                {
                  "autosaveSeconds": 120,
                  "commands": {"tools": {"enabled": false, "aliases": ["tc"]}},
                  "database": {"enabled": false, "host": "my.host"},
                  "customUserField": {"keepMe": true}
                }
                """);

        ToolsConfig result = load();
        assertEquals(120, result.autosaveSeconds());
        assertFalse(result.commands().tools().enabled());
        assertEquals("my.host", result.database().host());

        var json = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        assertTrue(json.getAsJsonObject("customUserField").get("keepMe").getAsBoolean());
        assertEquals("my.host", json.getAsJsonObject("database").get("host").getAsString());
        assertEquals("tools.admin",
                json.getAsJsonObject("commands").getAsJsonObject("tools").get("permission").getAsString());
    }

    @Test
    void leavesExistingFileUntouchedIfNothingChanged() throws IOException {
        Path file = tempDir.resolve("config.json");
        Files.writeString(file, "{ \"autosaveSeconds\": 30, \"commands\": {}, \"database\": {} }");
        load();
        String afterFirst = Files.readString(file);
        load();
        assertEquals(afterFirst, Files.readString(file));
    }

    @Test
    void doesNotOverwriteInvalidConfiguration() throws IOException {
        Path file = tempDir.resolve("config.json");
        String invalid = "{ \"autosaveSeconds\": 1 }";
        Files.writeString(file, invalid, StandardCharsets.UTF_8);
        assertThrows(IOException.class, this::load);
        assertEquals(invalid, Files.readString(file));
    }

    @Test
    void doesNotOverwriteMalformedJson() throws IOException {
        Path file = tempDir.resolve("config.json");
        String malformed = "{ \"database\": ";
        Files.writeString(file, malformed);
        assertThrows(IOException.class, this::load);
        assertEquals(malformed, Files.readString(file));
    }

    @Test
    void rejectsDirectoryTraversal() {
        var manager = new JsonConfigManager(tempDir);
        assertThrows(IllegalArgumentException.class, () ->
                manager.load("../secrets.json", ToolsConfig.class, ToolsConfig::new, ToolsConfig::validate));
    }
}

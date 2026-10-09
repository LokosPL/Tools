package pl.lokos.tools.registry;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ConfigRegistryTest {
    @TempDir
    Path folder;

    @Test
    void createsLaragonDefaultsOnNewServer() throws IOException {
        ConfigRegistry registry = new ConfigRegistry(folder);
        registry.loadAll();
        assertTrue(registry.tools().database().enabled());
        assertEquals("root", registry.tools().database().username());
        var json = JsonParser.parseString(Files.readString(folder.resolve("config.json"))).getAsJsonObject();
        assertEquals("", json.getAsJsonObject("database").get("password").getAsString());
    }

    @Test
    void migratesOldGeneratedConfigWithoutOverwritingOtherSettings() throws IOException {
        Files.writeString(folder.resolve("config.json"), """
                {"autosaveSeconds":60, "custom":{"preserve":true},
                 "database":{
                   "enabled":false,"host":"127.0.0.1","port":3306,"database":"tools",
                   "username":"tools","password":"${TOOLS_DB_PASSWORD}",
                   "sslMode":"PREFERRED","poolSize":6,"connectionTimeoutMs":5000
                 }}
                """);
        ConfigRegistry registry = new ConfigRegistry(folder);
        registry.loadAll();
        assertEquals(60, registry.tools().autosaveSeconds());
        assertTrue(registry.tools().database().enabled());
        assertEquals("root", registry.tools().database().username());
        var json = JsonParser.parseString(Files.readString(folder.resolve("config.json"))).getAsJsonObject();
        assertTrue(json.getAsJsonObject("custom").get("preserve").getAsBoolean());
    }
}

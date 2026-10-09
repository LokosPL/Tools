package pl.lokos.tools.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ToolsConfigMigrationTest {
    private static JsonObject previousDefaults() {
        return JsonParser.parseString("""
                {"database": {
                  "enabled": false, "host":"127.0.0.1", "port":3306, "database":"tools",
                  "username":"tools", "password":"${TOOLS_DB_PASSWORD}",
                  "sslMode":"PREFERRED", "poolSize":6, "connectionTimeoutMs":5000
                }}
                """).getAsJsonObject();
    }

    @Test
    void automaticallyUpgradesUntouchedOldDefaultsForLaragon() {
        JsonObject json = previousDefaults();
        ToolsConfigMigration.apply(json);
        JsonObject database = json.getAsJsonObject("database");
        assertTrue(database.get("enabled").getAsBoolean());
        assertEquals("root", database.get("username").getAsString());
        assertEquals("", database.get("password").getAsString());
        assertTrue(database.get("createDatabaseIfMissing").getAsBoolean());
    }

    @Test
    void doesNotOverrideCustomizedDatabase() {
        JsonObject json = previousDefaults();
        json.getAsJsonObject("database").addProperty("host", "192.168.1.3");
        ToolsConfigMigration.apply(json);
        assertFalse(json.getAsJsonObject("database").get("enabled").getAsBoolean());
        assertEquals("tools", json.getAsJsonObject("database").get("username").getAsString());
    }

    @Test
    void repeatedMigrationDoesNotChangeUpgradedSettings() {
        JsonObject json = previousDefaults();
        ToolsConfigMigration.apply(json);
        JsonObject upgraded = json.deepCopy();
        ToolsConfigMigration.apply(json);
        assertEquals(upgraded, json);
    }
}

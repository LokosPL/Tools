package pl.lokos.tools.registry;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class ConfigRegistryTest {
    @TempDir Path folder;

    @Test void createsFourIndependentFiles() throws IOException {
        ConfigRegistry config=new ConfigRegistry(folder);
        config.loadAll();
        for(String name:new String[]{"MySql.json","Commands.json","Ranks.json","Regions.json"})
            assertTrue(Files.exists(folder.resolve(name)),name);
        assertFalse(Files.exists(folder.resolve("config.json")));
        assertEquals("root",config.tools().database().username());
        assertTrue(config.commands().region().enabled());
        assertEquals("tools.ranga.admin",config.commands().ranga().permission());
        assertTrue(config.definitions().importRanks());
        assertEquals(50,config.tools().regions().spawnProtectionOutside());
    }

    @Test void migratesLegacyWithoutErasingUnknownSettings() throws IOException {
        Files.writeString(folder.resolve("config.json"),"""
                {"autosaveSeconds":60,
                 "database":{"enabled":true,"host":"192.168.1.2","port":3307,"database":"custom",
                 "username":"admin","password":"secret","sslMode":"PREFERRED","poolSize":6,
                 "connectionTimeoutMs":5000},
                 "commands":{"tools":{"enabled":true,"description":"Moje narzędzia",
                 "permission":"tools.admin","aliases":["narzedzia"]}},
                 "ranks":{"tabRefreshTicks":100},
                 "regions":{"teleportSeconds":7,"barTitle":"&aREGION:"}}
                """);
        ConfigRegistry config=new ConfigRegistry(folder);
        config.loadAll();
        assertEquals(60,config.tools().autosaveSeconds());
        assertEquals("192.168.1.2",config.tools().database().host());
        assertEquals(3307,config.tools().database().port());
        assertEquals("narzedzia",config.commands().tools().aliases().getFirst());
        assertEquals(7,config.tools().regions().teleportSeconds());
        assertEquals(100,config.tools().ranks().tabRefreshTicks());
        assertTrue(Files.exists(folder.resolve("config.json.legacy-backup")));
        assertFalse(Files.exists(folder.resolve("config.json")));
        assertEquals("secret",JsonParser.parseString(Files.readString(folder.resolve("MySql.json")))
                .getAsJsonObject().get("password").getAsString());
        config.loadAll();
        assertEquals(7,config.tools().regions().teleportSeconds());
    }

    @Test void existingNewJsonTakesPrecedenceOverLegacy() throws IOException {
        Files.writeString(folder.resolve("MySql.json"),"""
                {"host":"127.0.0.1","database":"tools","username":"root","password":"mine"}
                """);
        Files.writeString(folder.resolve("config.json"),"""
                {"database":{"host":"10.0.0.1","username":"wrong","password":"wrong"}}
                """);
        ConfigRegistry config=new ConfigRegistry(folder);
        config.loadAll();
        assertEquals("mine",config.tools().database().password());
    }
}

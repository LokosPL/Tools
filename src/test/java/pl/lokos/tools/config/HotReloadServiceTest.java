package pl.lokos.tools.config;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HotReloadServiceTest {
    private final Gson gson = new Gson();

    @Test void rejectsLiveDatabaseChange() {
        var oldDb = new ToolsConfig.Database();
        var newDb = gson.fromJson("{\"type\":\"SQLITE\"}", ToolsConfig.Database.class);
        var cmd = new CommandsFile(); var ranks = new RanksFile(); var regions = new RegionsFile();
        assertThrows(IllegalArgumentException.class, () ->
                HotReloadService.verifySafeToReload(oldDb, newDb, cmd, cmd, ranks, ranks, regions, regions));
    }

    @Test void acceptsMessageOnlyReload() {
        var db = new ToolsConfig.Database();
        var first = new CommandsFile();
        var second = gson.fromJson("{\"serverName\":\"NOWY\",\"messagePrefix\":\"&a\"}", CommandsFile.class);
        // Gson nie inicjuje pol prywatnych domyslnie bez argumentu? Obiekt ma inicjalizatory pól.
        second.validate();
        var ranks = new RanksFile(); var regions = new RegionsFile();
        assertDoesNotThrow(() -> HotReloadService.verifySafeToReload(
                db, db, first, second, ranks, ranks, regions, regions));
    }

    @Test void rejectsPermissionChange() {
        var db = new ToolsConfig.Database(); var cmd = new CommandsFile();
        var modified = gson.fromJson("{\"tools\":{\"enabled\":true,\"description\":\"Narzędzia administracyjne\","
                + "\"aliases\":[],\"permission\":\"tools.new\"}}", CommandsFile.class);
        var ranks = new RanksFile(); var regions = new RegionsFile();
        assertThrows(IllegalArgumentException.class, () -> HotReloadService.verifySafeToReload(
                db, db, cmd, modified, ranks, ranks, regions, regions));
    }
}

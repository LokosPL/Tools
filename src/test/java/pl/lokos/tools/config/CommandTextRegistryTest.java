package pl.lokos.tools.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CommandTextRegistryTest {
    @TempDir Path temp;

    @Test void generatesSeparateJsonFilesForEveryCommand() throws Exception {
        var registry=new CommandTextRegistry(temp);
        var loaded=registry.loadSnapshot();
        registry.install(loaded);
        assertEquals(Set.copyOf(CommandTextFile.names()),loaded.keySet());
        for(String name:CommandTextFile.names()) {
            assertTrue(Files.exists(temp.resolve("commands").resolve(name+".json")));
            assertFalse(loaded.get(name).help().isEmpty());
        }
    }
    @Test void acceptsUserEditsAndRetainsPreviousOnInvalidInput() throws Exception {
        var registry=new CommandTextRegistry(temp);
        registry.install(registry.loadSnapshot());
        Path file=temp.resolve("commands").resolve("region.json");
        Files.writeString(file,Files.readString(file)
                .replace("ZARZĄDZANIE REGIONAMI","MÓJ REGION"));
        var changed=registry.loadSnapshot();
        assertEquals("MÓJ REGION",changed.get("region").title());
        registry.install(changed);
        String before=CommandTextRegistry.text("region","notReady");
        Files.writeString(file,"{niepoprawny");
        assertThrows(java.io.IOException.class,registry::loadSnapshot);
        assertEquals(before,CommandTextRegistry.text("region","notReady"));
    }
}
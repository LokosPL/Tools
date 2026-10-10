package pl.lokos.tools.config;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import pl.lokos.tools.security.ToolsAccess;
import static org.junit.jupiter.api.Assertions.*;

class CrateBrowseConfigTest {
    @Test void newPublicCommandsHaveSeparatePermissions(){
        CommandsFile commands=new CommandsFile();
        assertTrue(commands.skrzynie().enabled());
        assertTrue(commands.klucze().enabled());
        assertEquals("tools.skrzynie.use",commands.skrzynie().permission());
        assertEquals("tools.klucze.use",commands.klucze().permission());
        assertTrue(ToolsAccess.publicNode(commands.skrzynie().permission()));
        assertTrue(ToolsAccess.publicNode(commands.klucze().permission()));
        assertDoesNotThrow(commands::validate);
    }
    @Test void previouslySavedCommandsStillValidate(){
        CommandsFile commands=new Gson().fromJson("{}",CommandsFile.class);
        assertDoesNotThrow(commands::validate);
    }
}

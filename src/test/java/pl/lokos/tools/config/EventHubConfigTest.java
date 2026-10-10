package pl.lokos.tools.config;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import pl.lokos.tools.security.ToolsAccess;
import static org.junit.jupiter.api.Assertions.*;

class EventHubConfigTest {
    @Test void publicEventHubHasIndependentCommandConfiguration(){
        CommandsFile commands=new CommandsFile();
        assertEquals("tools.eventy.use",commands.eventy().permission());
        assertTrue(commands.eventy().enabled());
        assertTrue(ToolsAccess.publicNode(commands.eventy().permission()));
        assertDoesNotThrow(commands::validate);
    }
    @Test void oldCommandConfigsGainEventHubDefaults(){
        CommandsFile commands=new Gson().fromJson(
                "{\"event\":{\"enabled\":true,\"description\":\"Admin\",\"aliases\":[],\"permission\":\"tools.event.admin\"}}",
                CommandsFile.class);
        assertDoesNotThrow(commands::validate);
        assertNotNull(commands.eventy());
    }
}

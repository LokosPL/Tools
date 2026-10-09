package pl.lokos.tools.listeners;

import org.junit.jupiter.api.Test;
import com.google.gson.Gson;
import pl.lokos.tools.config.CommandsFile;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ToolsCommandVisibilityListenerTest {
    @Test void hasAllRegisteredRootsAndAliasesForHiddenCommandFiltering(){
        ToolsCommandVisibilityListener listener=new ToolsCommandVisibilityListener(new CommandsFile(),null);
        Map<String,String> perms=listener.registeredRoots();
        assertEquals("tools.region.admin",perms.get("region"));
        assertEquals("tools.region.admin",perms.get("tools:region"));
        assertEquals("tools.lokalizacje",perms.get("lokalizacje"));
        assertEquals("tools.lokalizacje",perms.get("lokacje"));
        assertFalse(perms.containsKey("plugins"));
        assertFalse(perms.containsKey("minecraft:help"));
    }

    @Test void disabledCommandNeverGetsRegisteredForInterception(){
        CommandsFile commands=new Gson().fromJson("""
                {"region":{"enabled":false,"description":"Regiony","aliases":[],
                           "permission":"tools.region.admin"}}
                """,CommandsFile.class);
        commands.validate();
        ToolsCommandVisibilityListener listener=new ToolsCommandVisibilityListener(commands,null);
        assertFalse(listener.registeredRoots().containsKey("region"));
        assertTrue(listener.registeredRoots().containsKey("lokalizacje"));
    }
}

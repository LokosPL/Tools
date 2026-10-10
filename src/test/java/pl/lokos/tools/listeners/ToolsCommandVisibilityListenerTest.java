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
        assertEquals("tools.event.admin",perms.get("event"));
        assertEquals("tools.event.info",perms.get("zima"));
        assertEquals("tools.event.info",perms.get("halloween"));
        assertEquals("tools.event.info",perms.get("wielkanoc"));
        assertEquals("tools.skrzynia.admin",perms.get("skrzynia"));
        assertEquals("tools.granica.admin",perms.get("granica"));
        assertEquals("tools.tp",perms.get("tp"));
        assertEquals("tools.vanish.use",perms.get("vanish"));
        assertEquals("tools.helpop.use",perms.get("helpop"));
        assertEquals("tools.gamemode",perms.get("gamemode"));
        assertEquals("tools.gamemode",perms.get("gm"));
        assertEquals("tools.fly",perms.get("fly"));
        assertEquals("tools.broadcast",perms.get("broadcast"));
        assertEquals("tools.inventoryopen",perms.get("inventoryopen"));
        assertEquals("tools.inventoryopen",perms.get("invopen"));
        assertEquals("tools.speed",perms.get("speed"));
        assertEquals("tools.region.admin",perms.get("region"));
        assertEquals("tools.region.admin",perms.get("tools:region"));
        assertEquals("tools.lokalizacje",perms.get("lokalizacje"));
        assertEquals("tools.lokalizacje",perms.get("lokacje"));
        assertEquals("tools.msg.use",perms.get("msg"));
        assertEquals("tools.msg.use",perms.get("tell"));
        assertEquals("tools.msg.use",perms.get("w"));
        assertEquals("tools.msg.use",perms.get("reply"));
        assertEquals("tools.msg.use",perms.get("r"));
        assertEquals("tools.msg.use",perms.get("replay"));
        assertEquals("tools.chat.admin",perms.get("chat"));
        assertEquals("tools.chat.admin",perms.get("czat"));
        assertEquals("tools.chat.admin",perms.get("tools:chat"));
        assertEquals("tools.spawn",perms.get("spawn"));
        assertEquals("tools.spawn",perms.get("tools:spawn"));
        assertEquals("tools.whitelist.admin",perms.get("whitelist"));
        assertEquals("tools.whitelist.admin",perms.get("minecraft:whitelist"));
        assertEquals("tools.whitelist.admin",perms.get("wl"));
        assertEquals("tools.whitelist.admin",perms.get("tools:whitelist"));
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

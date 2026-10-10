package pl.lokos.tools.config;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CommandsFileTest {
    @Test void ToolsBorderNeverHijacksNativeWorldBorderCommand(){
        assertTrue(new CommandsFile().granica().aliases().isEmpty());
    }
    @Test void userCanCustomizeGlobalPrefix(){
        CommandsFile config=new Gson().fromJson("""
                {"serverName":"Przygoda","messagePrefix":"&8[&a{server}&8] ",
                 "region":{"description":"Regiony","permission":"tools.region.admin",
                   "aliases":[],"enabled":true}}
                """,CommandsFile.class);
        assertDoesNotThrow(config::validate);
        assertEquals("&8[&aPrzygoda&8] ",config.messagePrefix());
    }
}

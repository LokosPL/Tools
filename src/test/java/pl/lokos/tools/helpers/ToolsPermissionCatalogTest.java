package pl.lokos.tools.helpers;

import org.junit.jupiter.api.Test;
import pl.lokos.tools.config.CommandsFile;
import com.google.gson.Gson;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ToolsPermissionCatalogTest {
    @Test void noBukkitVanillaOrOtherPluginPermissionCanLeakIntoGuiOrSuggestions(){
        ToolsPermissionCatalog catalog=new ToolsPermissionCatalog(new CommandsFile());
        assertEquals(Set.of("tools.lokalizacje","tools.lokalizacje.instant",
                        "tools.region.admin","tools.region.bypass",
                        "tools.ranga.admin","tools.admin","*"),
                Set.copyOf(catalog.suggestions()));
        assertFalse(catalog.isManaged("bukkit.command.plugins"));
        assertFalse(catalog.isManaged("minecraft.command.help"));
        assertFalse(catalog.isManaged("luckperms.user.permission.set"));
        for(var feature:catalog.features()){
            assertFalse(feature.name().isBlank());
            assertTrue(feature.description().length()>14);
            assertFalse(feature.details().isBlank());
        }
    }

    @Test void rankPermissionMustActuallyBeEnabledUnlessPlayerIsOperator(){
        String feature="tools.lokalizacje";
        assertFalse(ToolsPermissionCatalog.granted(Set.of(),false,feature));
        assertFalse(ToolsPermissionCatalog.granted(Set.of("tools.lokalizacje.instant"),false,feature));
        assertTrue(ToolsPermissionCatalog.granted(Set.of(feature),false,feature));
        assertTrue(ToolsPermissionCatalog.granted(Set.of("*"),false,feature));
        assertTrue(ToolsPermissionCatalog.granted(Set.of(),true,feature));
    }

    @Test void configuredCommandPermissionsAppearInsteadOfHardCodedOnes(){
        CommandsFile config=new Gson().fromJson("""
                {"lokalizacje":{"enabled":true,"description":"Lokalizacje",
                                 "aliases":["lokacje"],"permission":"tools.lokacje.custom"}}
                """, CommandsFile.class);
        config.validate();
        ToolsPermissionCatalog catalog=new ToolsPermissionCatalog(config);
        assertTrue(catalog.isManaged("tools.lokacje.custom"));
        assertFalse(catalog.isManaged("tools.lokalizacje"));
        assertTrue(catalog.features().stream().anyMatch(f->
                f.permission().equals("tools.lokacje.custom") &&
                f.name().equals("Menu lokalizacji")));
    }
}

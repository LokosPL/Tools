package pl.lokos.tools.manager;

import org.junit.jupiter.api.Test;
import com.google.gson.Gson;
import pl.lokos.tools.config.RanksFile;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PublicGuiRankMigrationTest {
    @Test void publicPermissionsAreAddedWithoutLosingExistingRankSettings(){
        var old=new RanksFile.RankEntry("&7Gracz","&7!",9999,"Cześć",
                Set.of("tools.spawn","tools.lokalizacje"));
        var map=new LinkedHashMap<String,RanksFile.RankEntry>();
        map.put("gracz",old);
        RankManager.grantPublicGuiOnce(map);
        var added=map.get("gracz");
        assertEquals(old.prefix(),added.prefix());
        assertEquals(old.suffix(),added.suffix());
        assertEquals(old.position(),added.position());
        assertTrue(added.permissions().containsAll(Set.of("tools.spawn","tools.lokalizacje",
                "tools.event.info","tools.eventy.use","tools.skrzynie.use","tools.klucze.use")));
    }
    @Test void newRankFilesCarryMigrationMarker(){
        RanksFile file=RanksFile.from(Map.of(),new RanksFile().settings());
        assertTrue(file.publicGuiDefaultsInstalled());
        RanksFile copy=new Gson().fromJson(new Gson().toJson(file),RanksFile.class);
        assertTrue(copy.publicGuiDefaultsInstalled());
        RanksFile legacy=new Gson().fromJson("{}",RanksFile.class);
        assertFalse(legacy.publicGuiDefaultsInstalled());
    }
}

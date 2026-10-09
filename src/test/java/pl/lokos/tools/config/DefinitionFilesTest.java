package pl.lokos.tools.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.lokos.tools.region.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class DefinitionFilesTest {
    @TempDir Path temp;
    @Test void filesStoreDefinitionsAndDoNotStorePlayerAssignments() throws Exception {
        RanksFile file=RanksFile.from(Map.of("vip",new RanksFile.RankEntry(
                "&aVIP","",1,"",Set.of("essentials.fly"))),new ToolsConfig.Ranks());
        RegionsFile regions=RegionsFile.from(List.of(new Region("spawn",UUID.randomUUID(),
                -100,100,-100,100,null,null,Map.of(),null)),null,new ToolsConfig.Regions());
        DefinitionFiles definitions=new DefinitionFiles(temp,new RanksFile(),new RegionsFile(),true,true);
        definitions.saveRanks(file);
        definitions.saveRegions(regions);
        String rankJson=Files.readString(temp.resolve("Ranks.json"));
        String regionJson=Files.readString(temp.resolve("Regions.json"));
        assertTrue(rankJson.contains("essentials.fly"));
        assertTrue(rankJson.contains("vip"));
        assertTrue(regionJson.contains("spawn"));
        assertFalse(rankJson.contains("player_uuid"));
        assertFalse(regionJson.contains("tools_player_ranks"));
    }
}

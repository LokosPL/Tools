package pl.lokos.tools.region;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class RegionAdvancedTest {
    @Test void childCanEnableChestWithoutEnablingGenericInteraction(){
        UUID world=UUID.randomUUID();
        Region root=new Region("spawn",world,-100,100,-100,100,null,null,
                Map.of(RegionFlag.INTERACT,false),null);
        Region child=new Region("sklep",world,-10,10,-10,10,"spawn",null,
                Map.of(RegionFlag.CHESTS,true),null);
        RegionIndex index=new RegionIndex(List.of(root,child));
        assertTrue(index.enabled(child,RegionFlag.CHESTS));
        assertFalse(index.enabled(child,RegionFlag.CRAFTING));
        assertFalse(index.enabled(child,RegionFlag.INTERACT));
    }
    @Test void olderGenericInteractFlagAllowsSpecificInteractionsUnlessOverridden(){
        UUID world=UUID.randomUUID();
        Region root=new Region("spawn",world,-100,100,-100,100,null,null,
                Map.of(RegionFlag.INTERACT,true),null);
        Region child=new Region("afk",world,-10,10,-10,10,"spawn",null,
                Map.of(RegionFlag.CHESTS,false),null);
        RegionIndex index=new RegionIndex(List.of(root,child));
        assertTrue(index.enabled(child,RegionFlag.CRAFTING));
        assertFalse(index.enabled(child,RegionFlag.CHESTS));
    }
    @Test void allFlagLabelsResolveFromGuiAndCommands(){
        for(RegionFlag flag:RegionFlag.values()){
            assertEquals(flag,RegionFlag.parse(flag.label()));
            assertEquals(flag,RegionFlag.parse(flag.name()));
            assertFalse(flag.category().isBlank());
        }
    }
}

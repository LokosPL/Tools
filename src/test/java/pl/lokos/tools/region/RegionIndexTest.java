package pl.lokos.tools.region;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class RegionIndexTest {
    private static final UUID WORLD=UUID.randomUUID();

    @Test void regionIsSquareAtConfiguredRadiusAndIgnoresHeight() {
        Region root=new Region("spawn",WORLD,-100,100,-100,100,null,null,Map.of(),null);
        assertEquals(201L*201L,root.area());
        assertTrue(root.contains(WORLD,100,100));
        assertTrue(root.contains(WORLD,-100,-100));
        assertFalse(root.contains(WORLD,101,0));
        assertFalse(root.contains(UUID.randomUUID(),0,0));
    }
    @Test void childPriorityAndInheritance() {
        Region root=new Region("spawn",WORLD,-100,100,-100,100,null,null,Map.of(),null);
        Region pvp=new Region("arena",WORLD,-15,15,-15,15,"spawn",null,
                Map.of(RegionFlag.PVP,true),null);
        RegionIndex index=new RegionIndex(List.of(root,pvp));
        assertEquals("arena",index.at(WORLD,0,0).name());
        assertEquals("spawn",index.at(WORLD,60,0).name());
        assertTrue(index.enabled(pvp,RegionFlag.PVP));
        assertFalse(index.enabled(pvp,RegionFlag.BUILD));
        assertFalse(index.enabled(root,RegionFlag.PVP));
    }
    @Test void overlappingRootAndOutOfBoundsChildRejected() {
        Region root=new Region("spawn",WORLD,-50,50,-50,50,null,null,Map.of(),null);
        RegionIndex idx=new RegionIndex(List.of(root));
        Region overlap=new Region("shop",WORLD,30,60,30,60,null,null,Map.of(),null);
        assertThrows(IllegalArgumentException.class,()->idx.validateNew(overlap));
        Region outside=new Region("outside",WORLD,40,55,0,10,"spawn",null,Map.of(),null);
        assertThrows(IllegalArgumentException.class,()->idx.validateNew(outside));
    }
    @Test void explicitlyDisabledChildFlagOverridesEnabledParentFlag() {
        Region parent=new Region("spawn",WORLD,0,100,0,100,null,null,Map.of(RegionFlag.PVP,true),null);
        Region inner=new Region("safe",WORLD,20,40,20,40,"spawn",null,Map.of(RegionFlag.PVP,false),null);
        assertFalse(new RegionIndex(List.of(parent,inner)).enabled(inner,RegionFlag.PVP));
    }
    @Test void fluidCannotCrossProtectedBorder() {
        Region r=new Region("spawn",WORLD,-50,50,-50,50,null,null,Map.of(),null);
        RegionIndex idx=new RegionIndex(List.of(r));
        assertFalse(RegionPolicy.mayMove(idx,WORLD,51,0,50,0,RegionFlag.FLUIDS));
        assertFalse(RegionPolicy.mayMove(idx,WORLD,50,0,51,0,RegionFlag.FLUIDS));
        assertTrue(RegionPolicy.mayMove(idx,WORLD,80,0,81,0,RegionFlag.FLUIDS));
    }
}

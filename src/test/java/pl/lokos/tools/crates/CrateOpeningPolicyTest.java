package pl.lokos.tools.crates;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class CrateOpeningPolicyTest {
    @Test void onlyNearbyPlayerInMatchingWorldCanOpen(){
        UUID world=UUID.randomUUID();
        CratesState.Position p=new CratesState.Position(world.toString(),10,64,20,"zwykla");
        assertTrue(CrateOpeningPolicy.near(p,world,10.5,65.5,20.5));
        assertTrue(CrateOpeningPolicy.near(p,world,16.5,64.5,20.5));
        assertFalse(CrateOpeningPolicy.near(p,world,17,64.5,20.5));
        assertFalse(CrateOpeningPolicy.near(p,UUID.randomUUID(),10.5,65.5,20.5));
        assertFalse(CrateOpeningPolicy.near(p,world,10.5,180,20.5));
        assertFalse(CrateOpeningPolicy.near(null,world,10.5,65.5,20.5));
        assertFalse(CrateOpeningPolicy.near(p,world,Double.NaN,65.5,20.5));
    }
}

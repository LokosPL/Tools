package pl.lokos.tools.region;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RegionFlagCycleTest {
    @Test
    void transitionsThroughAllThreeStatesWithoutUnboxingNull() {
        Boolean state = null;
        state = RegionFlagCycle.next(state);
        assertEquals(Boolean.TRUE, state);
        state = RegionFlagCycle.next(state);
        assertEquals(Boolean.FALSE, state);
        state = RegionFlagCycle.next(state);
        assertNull(state);
        state = RegionFlagCycle.next(state);
        assertEquals(Boolean.TRUE, state);
    }

    @Test
    void inheritedFlagCanBeToggledOnFirstClick() {
        assertDoesNotThrow(() -> RegionFlagCycle.next(null));
        assertSame(Boolean.TRUE, RegionFlagCycle.next(null));
    }
}

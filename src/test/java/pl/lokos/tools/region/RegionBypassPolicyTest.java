package pl.lokos.tools.region;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RegionBypassPolicyTest {
    @Test void operatorAloneDoesNotBypass() {
        assertFalse(RegionBypassPolicy.active(true,false));
        assertFalse(RegionBypassPolicy.active(false,true));
        assertTrue(RegionBypassPolicy.active(true,true));
    }
}
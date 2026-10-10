package pl.lokos.tools.events;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class PvPKillPolicyTest {
    @Test void onlySurvivalDeathsOutsideProtectedRegionsCount(){
        UUID killer=UUID.randomUUID(),victim=UUID.randomUUID();
        assertTrue(PvPKillPolicy.eligible(killer,victim,true,true,false,false));
        assertFalse(PvPKillPolicy.eligible(killer,killer,true,true,false,false));
        assertFalse(PvPKillPolicy.eligible(killer,victim,false,true,false,false));
        assertFalse(PvPKillPolicy.eligible(killer,victim,true,false,false,false));
        assertFalse(PvPKillPolicy.eligible(killer,victim,true,true,true,false));
        assertFalse(PvPKillPolicy.eligible(killer,victim,true,true,false,true));
        assertFalse(PvPKillPolicy.eligible(null,victim,true,true,false,false));
    }
}

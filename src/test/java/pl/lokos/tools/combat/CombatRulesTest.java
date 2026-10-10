package pl.lokos.tools.combat;

import org.bukkit.event.entity.EntityDamageEvent;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CombatRulesTest {
    @Test void excludesSelfDamageCancelledDamageAndEnvironment(){
        assertFalse(CombatRules.shouldTag(false,true,false,true,true,true,true));
        assertFalse(CombatRules.shouldTag(true,true,true,true,true,true,true));
        assertFalse(CombatRules.shouldTag(false,false,true,true,true,true,true));
        assertFalse(CombatRules.shouldTag(false,true,true,false,true,true,true));
    }
    @Test void bothPlayerAndMobCombatCanTag(){
        assertTrue(CombatRules.shouldTag(false,true,true,true,true,true,true));
        assertTrue(CombatRules.shouldTag(false,true,true,true,false,true,true));
        assertFalse(CombatRules.shouldTag(false,true,true,true,false,true,false));
        assertFalse(CombatRules.shouldTag(false,true,true,true,true,false,true));
    }
    @Test void killingPassiveAnimalsDoesNotStartLogoutCombat(){
        assertFalse(CombatRules.shouldTagAttacker(false,false));
        assertTrue(CombatRules.shouldTagAttacker(true,false));
        assertTrue(CombatRules.shouldTagAttacker(false,true));
    }
    @Test void polishDeathCausesStayDeterministic(){
        assertTrue(CombatRules.reason(EntityDamageEvent.DamageCause.LAVA).contains("lawie"));
        assertTrue(CombatRules.reason(EntityDamageEvent.DamageCause.FALL).contains("upadku"));
        assertTrue(CombatRules.reason(null).contains("zginął"));
    }
}

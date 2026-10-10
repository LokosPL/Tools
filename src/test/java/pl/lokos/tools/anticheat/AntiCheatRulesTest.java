package pl.lokos.tools.anticheat;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AntiCheatRulesTest {
    @Test void boundedRateWindowAllowsNormalActivityAndRejectsBurst(){
        var limiter=new RateWindow<String>(1000,3);
        assertFalse(limiter.exceeded("one",2,10000));
        assertFalse(limiter.exceeded("one",2,10001));
        assertTrue(limiter.exceeded("one",2,10002));
        assertFalse(limiter.exceeded("one",2,11000));
        assertFalse(limiter.exceeded("two",1,12000));
        assertFalse(limiter.exceeded("three",1,12000));
        assertFalse(limiter.exceeded("four",1,12000));
        assertTrue(limiter.size()<=3);
        limiter.cleanup(13000);
        assertEquals(0,limiter.size());
    }
    @Test void opStatusNotInStateOrDetectionConfiguration(){
        var json=new com.google.gson.Gson();
        var defaults=json.fromJson("{}",AntiCheatConfig.class);
        defaults.validate();
        assertTrue(defaults.movementCheck());
        assertTrue(defaults.explosionProtection());
        assertThrows(IllegalArgumentException.class,()->
                json.fromJson("{\"blocksPerSecond\":1}",AntiCheatConfig.class).validate());
        assertThrows(IllegalArgumentException.class,()->
                json.fromJson("{\"maximumAttackDistance\":2.5}",AntiCheatConfig.class).validate());
    }
    @Test void persistentPersonalNotificationsAreIndependentOfGlobalState(){
        UUID admin=UUID.randomUUID(),other=UUID.randomUUID();
        AntiCheatState original=new AntiCheatState();
        assertTrue(original.enabled());
        assertTrue(original.receives(admin));
        AntiCheatState mute=original.withAlerts(admin,false);
        assertFalse(mute.receives(admin));
        assertTrue(mute.receives(other));
        assertTrue(mute.withEnabled(false).silentViewers().contains(admin.toString()));
        assertFalse(mute.withEnabled(false).enabled());
        assertTrue(mute.withAlerts(admin,true).receives(admin));
        mute.validate();
    }
}

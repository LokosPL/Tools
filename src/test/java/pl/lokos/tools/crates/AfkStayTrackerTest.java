package pl.lokos.tools.crates;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class AfkStayTrackerTest {
    @Test void standingStillCountsAndLeavingResetsOnlyStreak(){
        AfkStayTracker tracker=new AfkStayTracker();
        UUID player=UUID.randomUUID();
        for(int i=1;i<=59;i++)assertFalse(tracker.tick(player));
        assertTrue(tracker.tick(player));
        assertEquals(60,tracker.seconds(player));
        assertEquals(3540,AfkStayTracker.remainingSeconds(1,tracker.seconds(player),60));
        tracker.leave(player);
        assertEquals(0,tracker.seconds(player));
        assertFalse(tracker.tick(player));
    }

    @Test void longerUninterruptedStayIncreasesKeysUntilCap(){
        assertEquals(1,AfkStayTracker.rewardKeys(60*60L,60,60,4));
        assertEquals(2,AfkStayTracker.rewardKeys(120*60L,60,60,4));
        assertEquals(3,AfkStayTracker.rewardKeys(180*60L,60,60,4));
        assertEquals(4,AfkStayTracker.rewardKeys(240*60L,60,60,4));
        assertEquals(4,AfkStayTracker.rewardKeys(1000*60L,60,60,4));
        assertEquals(1,AfkStayTracker.rewardKeys(60L,60,60,4));
        assertEquals(1,AfkStayTracker.rewardKeys(7200L,60,60,1));
        assertEquals(1,AfkStayTracker.rewardKeys(119*60L,60,90,4));
        assertEquals(2,AfkStayTracker.rewardKeys(150*60L,60,90,4));
    }

    @Test void countdownResumesSavedMinutesAndIncludesSeconds(){
        assertEquals(3600,AfkStayTracker.remainingSeconds(0,0,60));
        assertEquals(60,AfkStayTracker.remainingSeconds(59,0,60));
        assertEquals(1,AfkStayTracker.remainingSeconds(59,59,60));
        assertEquals(3540,AfkStayTracker.remainingSeconds(61,60,60));
        assertEquals("01:00:00",AfkStayTracker.countdown(3600));
        assertEquals("59:59",AfkStayTracker.countdown(3599));
        assertEquals("00:01",AfkStayTracker.countdown(1));
        assertThrows(IllegalArgumentException.class,
                ()->AfkStayTracker.remainingSeconds(-1,0,60));
    }
}

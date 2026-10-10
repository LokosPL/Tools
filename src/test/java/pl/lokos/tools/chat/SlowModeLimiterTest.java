package pl.lokos.tools.chat;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class SlowModeLimiterTest {
    @Test void onlyTwoMessagesEveryThreeSecondsSlidingWindow(){
        SlowModeLimiter limiter=new SlowModeLimiter();
        UUID id=UUID.randomUUID();
        assertTrue(limiter.allow(id,1_000L,3,2));
        assertTrue(limiter.allow(id,1_500L,3,2));
        assertFalse(limiter.allow(id,2_000L,3,2));
        assertFalse(limiter.allow(id,3_999L,3,2));
        assertTrue(limiter.allow(id,4_000L,3,2));
        assertFalse(limiter.allow(id,4_001L,3,2));
        assertTrue(limiter.allow(id,4_500L,3,2));
        assertFalse(limiter.allow(id,4_501L,3,2));
    }
    @Test void independentPlayersAndQuitReset(){
        SlowModeLimiter limiter=new SlowModeLimiter();
        UUID first=UUID.randomUUID(),second=UUID.randomUUID();
        assertTrue(limiter.allow(first,1000,3,1));
        assertFalse(limiter.allow(first,1000,3,1));
        assertTrue(limiter.allow(second,1000,3,1));
        limiter.remove(first);
        assertTrue(limiter.allow(first,1200,3,1));
    }
}

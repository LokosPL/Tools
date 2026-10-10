package pl.lokos.tools.crates;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class CrateActivityWindowTest {
    @Test void idlePlayerStopsEarningEvenWhileOnline(){
        CrateActivityWindow window=new CrateActivityWindow();
        UUID player=UUID.randomUUID();
        assertFalse(window.recentlyActive(player,600_000L,5));
        window.record(player,600_000L);
        assertTrue(window.recentlyActive(player,899_999L,5));
        assertFalse(window.recentlyActive(player,900_000L,5));
        assertFalse(window.recentlyActive(player,900_001L,5));
    }
    @Test void realActionRestartsWindowButLogoutClearsSession(){
        CrateActivityWindow window=new CrateActivityWindow();
        UUID player=UUID.randomUUID();
        window.record(player,100_000L);
        assertFalse(window.recentlyActive(player,500_000L,5));
        window.record(player,500_000L);
        assertTrue(window.recentlyActive(player,500_001L,5));
        window.remove(player);
        assertFalse(window.recentlyActive(player,500_002L,5));
    }
    @Test void recordsAreScopedPerPlayerAndRejectBadWindow(){
        CrateActivityWindow window=new CrateActivityWindow();
        UUID a=UUID.randomUUID(),b=UUID.randomUUID();
        window.record(a,1000L);
        assertTrue(window.recentlyActive(a,1100L,1));
        assertFalse(window.recentlyActive(b,1100L,1));
        assertFalse(window.recentlyActive(a,999L,1));
        assertThrows(IllegalArgumentException.class,()->window.recentlyActive(a,1100L,0));
        assertThrows(IllegalArgumentException.class,()->window.recentlyActive(a,1100L,61));
        window.clear();
        assertFalse(window.recentlyActive(a,1100L,1));
    }
}

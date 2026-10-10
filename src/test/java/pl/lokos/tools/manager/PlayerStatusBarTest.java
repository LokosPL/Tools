package pl.lokos.tools.manager;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlayerStatusBarTest {
    @Test void alwaysKeepsLocationAndProgressWithTemporaryAlert() {
        String result=PlayerStatusBar.format("spawn",5,49,
                "&#FF6B79✘ Obszar chroniony");
        assertTrue(result.contains("spawn"));
        assertTrue(result.contains("Poziom:"));
        assertTrue(result.contains("49%"));
        assertTrue(result.contains("Obszar chroniony"));
        assertTrue(result.contains("&#FF6B79"));
    }
    @Test void outsideRegionAndProgressAreSafe() {
        assertTrue(PlayerStatusBar.format(null,-1,120,null).contains("Dzicz"));
        assertTrue(PlayerStatusBar.format("jaskinia",7,120,"").contains("100%"));
        assertFalse(PlayerStatusBar.format("jaskinia",7,120,"").contains("Obszar chroniony"));
        assertTrue(PlayerStatusBar.format("s".repeat(100),0,-5,"").contains("…"));
    }
}

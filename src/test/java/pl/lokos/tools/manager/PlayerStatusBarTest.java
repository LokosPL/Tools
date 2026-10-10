package pl.lokos.tools.manager;

import org.junit.jupiter.api.Test;
import pl.lokos.tools.config.ToolsConfig;
import static org.junit.jupiter.api.Assertions.*;

class PlayerStatusBarTest {
    private final ToolsConfig.ActionBar settings = new ToolsConfig.ActionBar();

    @Test void regionOnlyDoesNotLeakLevelProgressOrWarehouse() {
        String result = PlayerStatusBar.format("Spawn", null, settings);
        assertTrue(result.contains("✦"));
        assertTrue(result.contains("Lokalizacja:"));
        assertTrue(result.contains("Spawn"));
        assertFalse(result.contains("Poziom"));
        assertFalse(result.contains("Postęp"));
        assertFalse(result.contains("magazyn"));
    }

    @Test void noRegionNoNoticeMeansNoPacket() {
        assertNull(PlayerStatusBar.format(null, null, settings));
        assertNull(PlayerStatusBar.format("", "", settings));
        assertNull(PlayerStatusBar.format(null, "  ", settings));
    }

    @Test void protectionNoticeIsTemporarySideMessage() {
        String alert = PlayerStatusBar.format("Spawn", settings.protectedMessage(), settings);
        assertTrue(alert.contains("Lokalizacja:"));
        assertTrue(alert.contains("Obszar chroniony"));
        assertTrue(alert.contains("&#FF727F"));
        String clean = PlayerStatusBar.format("Spawn", null, settings);
        assertFalse(clean.contains("Obszar chroniony"));
    }

    @Test void outsideRegionCanDisplayAlertWithoutFakeLocation() {
        String notice = PlayerStatusBar.format(null, settings.protectedMessage(), settings);
        assertNotNull(notice);
        assertFalse(notice.contains("Lokalizacja"));
        assertFalse(notice.contains("Dzicz"));
        assertTrue(PlayerStatusBar.format("s".repeat(100), "", settings).contains("…"));
        assertFalse(PlayerStatusBar.format("spawn&c", null, settings).contains("spawn&c"));
    }
}

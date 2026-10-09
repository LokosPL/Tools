package pl.lokos.tools.manager;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;
import pl.lokos.tools.config.ToolsConfig;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TabPanelTest {
    private static final ToolsConfig.Ranks SETTINGS = new ToolsConfig.Ranks();
    private static final TabPanel.Stats SAMPLE =
            new TabPanel.Stats("LokosPL", "właściciel", 3, 100, 28, 15, 4, 72_000, 19.92);

    @Test
    void dynamicHeaderIncludesOnlineTpsAndViewerProfile() {
        String plain = PlainTextComponentSerializer.plainText().serialize(TabPanel.header(SETTINGS, SAMPLE));
        assertTrue(plain.contains("3/100"));
        assertTrue(plain.contains("19.9"));
        assertTrue(plain.contains("właściciel"));
        assertTrue(plain.contains("28 ms"));
    }

    @Test
    void footerIncludesPersonalStatsAndSortedLeaderboard() {
        String plain = PlainTextComponentSerializer.plainText().serialize(
                TabPanel.footer(SETTINGS, SAMPLE, List.of(
                        new TabPanel.TopPlayer("LokosPL", 15),
                        new TabPanel.TopPlayer("Steve", 10)
                )));
        assertTrue(plain.contains("Zabójstwa: 15"));
        assertTrue(plain.contains("Śmierci: 4"));
        assertTrue(plain.contains("1g 0min"));
        assertTrue(plain.indexOf("LokosPL") < plain.indexOf("Steve"));
    }

    @Test
    void playtimeShowsMinutesHoursDays() {
        assertEquals("0min", TabPanel.playtime(0));
        assertEquals("1g 0min", TabPanel.playtime(72_000));
        assertEquals("1d 0g 0min", TabPanel.playtime(86_400 * 20L));
    }
}

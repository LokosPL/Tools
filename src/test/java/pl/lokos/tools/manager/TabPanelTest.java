package pl.lokos.tools.manager;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;
import pl.lokos.tools.config.ToolsConfig;

import static org.junit.jupiter.api.Assertions.*;

class TabPanelTest {
    private static final ToolsConfig.Ranks SETTINGS = new ToolsConfig.Ranks();
    private static final TabPanel.Stats SAMPLE =
            new TabPanel.Stats("LokosPL", "właściciel", 3, 100, 28, 15, 4, 72_000, 19.92);

    @Test
    void classicHeaderShowsSmallCapsAndOnlineCount() {
        String plain = PlainTextComponentSerializer.plainText().serialize(TabPanel.header(SETTINGS, SAMPLE));
        assertTrue(plain.contains("TOOLS"));
        assertTrue(plain.contains("ᴀᴋᴛᴜᴀʟɴɪᴇ ɢʀᴀᴄᴢʏ"));
        assertTrue(plain.contains("3/100"));
        assertTrue(plain.lines().count() <= 6);
    }

    @Test
    void classicFooterShowsPersonalStatisticsNotArtificialLeaderboard() {
        String plain = PlainTextComponentSerializer.plainText().serialize(TabPanel.footer(SETTINGS, SAMPLE));
        assertTrue(plain.contains("właściciel"));
        assertTrue(plain.contains("15"));
        assertTrue(plain.contains("28 ms"));
        assertTrue(plain.contains("1g 0min"));
        assertFalse(plain.contains("TOP ZABÓJSTW"));
        assertTrue(plain.lines().count() <= 8);
    }

    @Test
    void standardTabIsCompactEvenWithOnePlayer() {
        var single = new TabPanel.Stats("LokosPL", "Gracz", 1, 20, 35, 3, 1, 72000, 19.6);
        String header = PlainTextComponentSerializer.plainText().serialize(TabPanel.header(SETTINGS, single));
        String footer = PlainTextComponentSerializer.plainText().serialize(TabPanel.footer(SETTINGS, single));
        assertTrue(header.contains("1/20"));
        assertFalse(header.contains("┃"));
        assertFalse(footer.contains("┃"));
        assertTrue(footer.contains("Gracz"));
    }

    @Test
    void playtimeShowsMinutesHoursDays() {
        assertEquals("0min", TabPanel.playtime(0));
        assertEquals("1g 0min", TabPanel.playtime(72000));
        assertEquals("1d 0g 0min", TabPanel.playtime(86_400 * 20L));
    }
}

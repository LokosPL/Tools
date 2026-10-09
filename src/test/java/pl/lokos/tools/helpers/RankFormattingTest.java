package pl.lokos.tools.helpers;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RankFormattingTest {
    @Test
    void ensuresExactlyNecessarySpacing() {
        assertEquals("&c[VIP] ", RankFormatting.prefix("&c[VIP]"));
        assertEquals("&c[VIP] ", RankFormatting.prefix("&c[VIP] "));
        assertEquals("", RankFormatting.prefix("&7"));
        assertEquals(" &6★", RankFormatting.suffix("&6★"));
        assertEquals(" &6★", RankFormatting.suffix(" &6★"));
        assertEquals("", RankFormatting.suffix("&7"));
        assertEquals("&#55AAFF[Admin] ", RankFormatting.prefix("&#55AAFF[Admin]"));
    }
}

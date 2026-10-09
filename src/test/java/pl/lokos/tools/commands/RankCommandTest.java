package pl.lokos.tools.commands;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RankCommandTest {
    @Test
    void parsesRelativeAndPermanentTime() {
        assertNull(RankCommand.parseTime("na_zawsze"));
        assertNull(RankCommand.parseTime("na zawsze"));
        long before = System.currentTimeMillis();
        long expiry = RankCommand.parseTime("7d");
        assertTrue(expiry >= before + 7L * 86_400_000);
        assertTrue(expiry <= System.currentTimeMillis() + 7L * 86_400_000);
    }

    @Test
    void acceptsPolishRankNamesAndNormalizesCase() {
        assertEquals("właściciel", RankCommand.rankName("Właściciel"));
        assertEquals("zażółć_gęślą", RankCommand.rankName("Zażółć_Gęślą"));
        assertEquals("vip", RankCommand.rankName("VIP"));
        assertThrows(IllegalArgumentException.class, () -> RankCommand.rankName("VIP Gracz"));
        assertThrows(IllegalArgumentException.class, () -> RankCommand.rankName("1234567890123456789012345"));
    }

    @Test
    void rejectsInvalidDuration() {
        assertThrows(IllegalArgumentException.class, () -> RankCommand.parseTime("10x"));
        assertThrows(IllegalArgumentException.class, () -> RankCommand.parseTime("-3d"));
        assertThrows(IllegalArgumentException.class, () -> RankCommand.parseTime("0m"));
    }
}

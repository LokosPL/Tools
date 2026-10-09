package pl.lokos.tools.helpers;

import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RankValidityTest {
    @Test
    void permanentIsNotZeroAndDatesUseServerZone() {
        assertEquals("na zawsze", RankValidity.remaining(null, 1000));
        assertEquals("na zawsze", RankValidity.durationLabel("*"));
        assertEquals("na zawsze", RankValidity.durationLabel("na_zawsze"));
        assertEquals("bez daty zakończenia", RankValidity.expirationDate(null, ZoneId.of("Europe/Warsaw")));
        assertTrue(RankValidity.expirationDate(1780000000000L, ZoneId.of("Europe/Warsaw")).contains("2026"));
    }

    @Test
    void remainingTimeUsesDaysHoursMinutes() {
        long now = 1000L;
        assertEquals("1 dzień, 2 godz.", RankValidity.remaining(now + 26 * 3600000L, now));
        assertEquals("45 min", RankValidity.remaining(now + 45 * 60000L, now));
        assertEquals("wygasła", RankValidity.remaining(now - 1, now));
        assertEquals("7 dni", RankValidity.durationLabel("7d"));
        assertEquals("1 dzień", RankValidity.durationLabel("1d"));
        assertEquals("2 tygodnie", RankValidity.durationLabel("2w"));
        assertEquals("5 tygodni", RankValidity.durationLabel("5w"));
    }
}

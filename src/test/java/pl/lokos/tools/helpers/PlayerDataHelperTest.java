package pl.lokos.tools.helpers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerDataHelperTest {
    @Test
    void formatsDuration() {
        assertEquals("0d 0h 0m 0s", PlayerDataHelper.formatPlaytime(-1000));
        assertEquals("1d 2h 3m 4s", PlayerDataHelper.formatPlaytime(93_784_000L));
    }
}

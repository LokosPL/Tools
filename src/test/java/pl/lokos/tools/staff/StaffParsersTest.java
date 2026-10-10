package pl.lokos.tools.staff;

import org.bukkit.GameMode;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StaffParsersTest {
    @Test void customModeNumbersOneToFourAndNames(){
        assertEquals(GameMode.SURVIVAL,StaffParsers.gameMode("1"));
        assertEquals(GameMode.CREATIVE,StaffParsers.gameMode("2"));
        assertEquals(GameMode.ADVENTURE,StaffParsers.gameMode("3"));
        assertEquals(GameMode.SPECTATOR,StaffParsers.gameMode("4"));
        assertEquals(GameMode.CREATIVE,StaffParsers.gameMode("creative"));
        assertThrows(IllegalArgumentException.class,()->StaffParsers.gameMode("5"));
    }
    @Test void teleportCoordinatesAllowRelativeButRejectNonFiniteAndOutOfBorder(){
        assertEquals(12.0,StaffParsers.coordinate("~2",10,29999984),0.001);
        assertEquals(10.0,StaffParsers.coordinate("~",10,29999984),0.001);
        assertEquals(-150.5,StaffParsers.coordinate("-150.5",0,29999984),0.001);
        assertThrows(IllegalArgumentException.class,()->StaffParsers.coordinate("NaN",0,29999984));
        assertThrows(IllegalArgumentException.class,()->StaffParsers.coordinate("Infinity",0,29999984));
        assertThrows(IllegalArgumentException.class,()->StaffParsers.coordinate("99999999999",0,29999984));
        assertThrows(IllegalArgumentException.class,()->StaffParsers.coordinate("x",0,29999984));
    }
    @Test void announcementsAcceptDaysHoursMinutesAndSeconds(){
        assertEquals(30,StaffParsers.duration("30s",86400));
        assertEquals(300,StaffParsers.duration("5m",86400));
        assertEquals(7200,StaffParsers.duration("2h",86400));
        assertEquals(86400,StaffParsers.duration("1d",86400));
        assertThrows(IllegalArgumentException.class,()->StaffParsers.duration("7d",86400));
        assertThrows(IllegalArgumentException.class,()->StaffParsers.duration("0s",86400));
        assertThrows(IllegalArgumentException.class,()->StaffParsers.duration("999999d",86400));
    }
    @Test void speedAlwaysWithinBukkitBounds(){
        assertEquals(0.1f,StaffParsers.speed("1"),0.0001);
        assertEquals(1.0f,StaffParsers.speed("10"),0.0001);
        assertThrows(IllegalArgumentException.class,()->StaffParsers.speed("0"));
        assertThrows(IllegalArgumentException.class,()->StaffParsers.speed("11"));
        assertThrows(IllegalArgumentException.class,()->StaffParsers.speed("abc"));
    }
}

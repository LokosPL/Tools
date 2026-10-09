package pl.lokos.tools.helpers;

import net.kyori.adventure.text.format.TextColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ColorsTest {
    @Test
    void parsesHexRgbAndLegacyColors() {
        assertEquals("&x&F&F&0&0&A&A", Colors.normalize("&#FF00AA"));
        assertEquals("A &x&1&2&3&4&5&6 B", Colors.normalize("A &#123456 B"));
        assertEquals("Witaj", Colors.plain("&aWitaj"));
        var component = Colors.color("&#123456Test");
        assertEquals(TextColor.color(0x123456), component.color());
    }

    @Test
    void doesNotAffectIncorrectHex() {
        assertEquals("&#abcdf", Colors.normalize("&#abcdf"));
    }
}

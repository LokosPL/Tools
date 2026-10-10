package pl.lokos.tools.helpers;

import org.junit.jupiter.api.Test;
import pl.lokos.tools.config.VisualsConfig;
import static org.junit.jupiter.api.Assertions.*;

class ColorsPaletteTest {
    @Test void legacyColorsUseServerPaletteButExplicitHexRemainsIntact(){
        try{
            Colors.configurePalette(new VisualsConfig());
            String result=Colors.normalize("&eZłoty &bTurkus &7Szary &#ABCDEFHEX");
            assertTrue(result.contains("&x&F&F&D&1&6&6"));
            assertTrue(result.contains("&x&7&0&D&6&E&8"));
            assertTrue(result.contains("&x&A&8&A&8&B&7"));
            assertTrue(result.contains("&x&A&B&C&D&E&F"));
            assertEquals("Złoty Turkus",Colors.plain("&eZłoty &bTurkus"));
        }finally{
            Colors.configurePalette(null);
        }
    }
}

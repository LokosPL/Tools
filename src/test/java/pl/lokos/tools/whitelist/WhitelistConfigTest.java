package pl.lokos.tools.whitelist;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class WhitelistConfigTest {
    @Test void defaultsAreSafeAndNamesAreCaseInsensitive() {
        WhitelistConfig defaults=new WhitelistConfig();
        defaults.validate();
        assertFalse(defaults.enabled());
        assertEquals("lokospl", WhitelistConfig.normalize("LokosPL"));
        assertEquals("Właściciel", "Właściciel"); // nazwy rang są niezależne od whitelisty.
        WhitelistConfig updated=defaults.with(true,"NOWA_EDYCJA",Set.of("lokospl"));
        assertTrue(updated.enabled());
        assertTrue(updated.players().contains("lokospl"));
        assertFalse(defaults.enabled());
    }
    @Test void rejectsInvalidNickAndMode() {
        assertThrows(IllegalArgumentException.class,()->WhitelistConfig.normalize("gracz z spacją"));
        assertThrows(IllegalArgumentException.class,()->WhitelistMode.parse("niewlasciwy"));
    }
}

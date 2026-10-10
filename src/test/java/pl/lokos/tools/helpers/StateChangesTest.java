package pl.lokos.tools.helpers;

import org.junit.jupiter.api.Test;
import java.util.concurrent.CompletionException;
import static org.junit.jupiter.api.Assertions.*;

class StateChangesTest {
    @Test void unchangedStateCanBeIdentifiedAcrossAsyncBoundaries() {
        StateChanges.Unchanged unchanged=assertThrows(StateChanges.Unchanged.class,
                ()->StateChanges.requireChange(true,"To jest już włączone."));
        assertSame(unchanged,StateChanges.root(new CompletionException(unchanged)));
        assertEquals("To jest już włączone.",unchanged.getMessage());
        assertDoesNotThrow(()->StateChanges.requireChange(false,"brak"));
    }
}
